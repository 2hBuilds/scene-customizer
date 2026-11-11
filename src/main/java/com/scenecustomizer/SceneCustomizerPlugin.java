package com.scenecustomizer;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.scenecustomizer.SceneCustomizerConfig.SidebarIcon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.TileObject;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 2h NPC & Object Customizer: the sidebar button and its panel, and the events that feed the recorder. Every event goes to the
 * recorder on the client thread; what the recorder says goes to the panel on the Swing thread.
 */
@PluginDescriptor(
	name = "2h NPC & Object Customizer",
	internalName = "scene-customizer",
	description = "Record a session, see every NPC and object with its picture, then outline or hide them by name. (v" + Version.CURRENT + ")",
	tags = {"highlight", "outline", "hide", "hider", "npc", "object", "marker", "record", "recorder", "wiki"}
)
public class SceneCustomizerPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(SceneCustomizerPlugin.class);

	/** The wiki setting's key. */
	private static final String USE_WIKI_KEY = "useWiki";

	/** The sidebar button's place. */
	private static final int NAV_PRIORITY = 7;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Client client;

	@Inject
	private Recorder recorder;

	@Inject
	private RenderCallbackManager renderCallbackManager;

	@Inject
	private ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Gson gson;

	@Inject
	private ColorPickerManager colorPickerManager;

	@Inject
	private SceneCustomizerConfig config;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private ScheduledExecutorService executor;

	/** Draws the look-up rows' pictures from the game's models; set in startUp. */
	private ModelPictures modelPictures;
	private Rules rules;
	/** Set in startUp and cleared in shutDown; the events run on the client thread, so it is volatile. */
	private volatile Engine engine;
	private HideCallback hideCallback;
	private OutlineOverlay outlineOverlay;

	private NavigationButton navButton;
	/** The picture the sidebar button shows now; Swing thread (and startUp). */
	private SidebarIcon shownIcon;
	private SceneCustomizerPanel panel;
	/** The look-up's brain; set in startUp and cleared in shutDown. */
	private volatile LookupFlow flow;
	private volatile LookupBar lookupBar;
	/** The look-up session being filled; touched on the client thread only. */
	private Session lookupSession;
	/** The generation of the look-up run lookupSession is filled from; results of any other are dropped. */
	private int lookupGeneration;
	/** True once the rows of the current look-up were made; later results only hand pictures to them. */
	private boolean lookupFilled;
	/** A picture landed since the views were last handed to the panel; client thread only. */
	private boolean picturesChanged;

	@Provides
	SceneCustomizerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SceneCustomizerConfig.class);
	}

	@Override
	protected void startUp()
	{
		dropStoredKey(configManager, "exampleSwitch", "the example setting was taken out of the settings");
		// Choice: the look-up draws with the recorder's picture maker, so both share one picture memory.
		modelPictures = recorder.pictures();
		modelPictures.setLandedListener(() -> picturesChanged = true);
		rules = new Rules(configManager, gson);
		rules.load();
		final LookupFlow lookupFlow = new LookupFlow(new WikiClient(okHttpClient, executor), executor, new WikiAnswers(gson),
			state -> SwingUtilities.invokeLater(() -> showBar(state)),
			result -> clientThread.invoke(() -> lookupArrived(result)));
		flow = lookupFlow;
		lookupBar = new LookupBar(text -> lookupFlow.typed(text, config.useWiki()), lookupFlow::pick);
		panel = new SceneCustomizerPanel(
			this::resetSettings,
			() -> clientThread.invoke(this::toggleRecording),
			rules, this::openColourPicker, SceneCustomizerPlugin::copyToClipboard, this::removeRow, lookupBar,
			config::useWiki, on -> configManager.setConfiguration(SceneCustomizerConfig.GROUP, USE_WIKI_KEY, on),
			config::sidebarIcon,
			icon -> configManager.setConfiguration(SceneCustomizerConfig.GROUP, SceneCustomizerConfig.SIDEBAR_ICON_KEY, icon));
		final SceneCustomizerPanel shown = panel;
		recorder.setStatusListener(text -> SwingUtilities.invokeLater(() -> shown.setStatus(text)));
		recorder.setStopListener(view ->
		{
			final List<SessionView> all = keptViews();
			SwingUtilities.invokeLater(() ->
			{
				shown.setRecording(false);
				shown.setStatus(RecordingText.header(view));
				shown.sessionsChanged(all, view);
			});
		});
		final Engine started = new Engine(client, rules);
		engine = started;
		hideCallback = new HideCallback(started.decisions());
		outlineOverlay = new OutlineOverlay(client, modelOutlineRenderer, started.decisions());
		renderCallbackManager.register(hideCallback);
		overlayManager.add(outlineOverlay);
		rules.addListener(() -> clientThread.invoke(started::rulesChanged));
		clientThread.invoke(started::rulesChanged);
		shownIcon = config.sidebarIcon();
		navButton = navigationButton(NavIcon.create(shownIcon));
		clientToolbar.addNavigation(navButton);
		log.info("2h NPC & Object Customizer {} started on RuneLite {}", Version.CURRENT, RuneLiteProperties.getVersion());
	}

	/** The sidebar button, with {@code icon}. */
	private NavigationButton navigationButton(BufferedImage icon)
	{
		return NavigationButton.builder()
			.tooltip("2h NPC & Object Customizer")
			.icon(icon)
			.priority(NAV_PRIORITY)
			.panel(panel)
			.build();
	}

	/** What the settings menu's "Reset to default" does: puts every setting back to its default. */
	private void resetSettings()
	{
		configManager.unsetConfiguration(SceneCustomizerConfig.GROUP, USE_WIKI_KEY);
		configManager.unsetConfiguration(SceneCustomizerConfig.GROUP, SceneCustomizerConfig.SIDEBAR_ICON_KEY);
	}

	/**
	 * Shows the picture the setting asks for (Swing thread). A button's icon cannot change, so a different picture swaps
	 * the button: the settings menu is closed first, and the panel is opened again, once the toolbar has done the swap, if it was showing. Nothing is swapped
	 * when the picture would not change, and nothing happens after {@code shutDown}, which has dropped the button.
	 */
	private void applyIcon()
	{
		final SceneCustomizerPanel shown = panel;
		if (navButton == null || shown == null)
		{
			return;
		}
		final SidebarIcon wanted = config.sidebarIcon();
		if (wanted == shownIcon)
		{
			return;
		}
		final boolean wasShowing = shown.isShowing();
		shown.closeMenu();
		final NavigationButton old = navButton;
		navButton = navigationButton(NavIcon.create(wanted));
		shownIcon = wanted;
		new IconSwap<NavigationButton>(clientToolbar::removeNavigation, clientToolbar::addNavigation,
			clientToolbar::openPanel, () -> navButton, SwingUtilities::invokeLater).swap(old, navButton, wasShowing);
	}

	@Override
	protected void shutDown()
	{
		final LookupFlow looking = flow;
		flow = null;
		lookupBar = null;
		if (looking != null)
		{
			looking.stop();
		}
		final Engine stopping = engine;
		engine = null;
		if (hideCallback != null)
		{
			renderCallbackManager.unregister(hideCallback);
			hideCallback = null;
		}
		if (outlineOverlay != null)
		{
			overlayManager.remove(outlineOverlay);
			outlineOverlay = null;
		}
		clientThread.invoke(() ->
		{
			recorder.stop(System.currentTimeMillis());
			recorder.endFinishing();
			picturesChanged = false;
			if (modelPictures != null)
			{
				modelPictures.setLandedListener(null);
				modelPictures.clearMemory();
			}
			if (stopping != null)
			{
				stopping.shutDown();
			}
		});
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
		panel = null;
		clientThread.invoke(() -> lookupSession = null);
	}

	/**
	 * Drops one stored key of this plugin's group: reads it, and when something is stored says why at INFO once and
	 * unsets it (STANDARD A6, removing a config key). A manager that is missing or throws does not stop the plugin.
	 */
	public static void dropStoredKey(ConfigManager manager, String key, String why)
	{
		if (manager == null)
		{
			return;
		}
		try
		{
			final String stored = manager.getConfiguration(SceneCustomizerConfig.GROUP, key);
			if (stored == null || stored.isEmpty())
			{
				return;
			}
			log.info("Dropping the stored setting '{}': {}", key, why);
			manager.unsetConfiguration(SceneCustomizerConfig.GROUP, key);
		}
		catch (RuntimeException e)
		{
			log.debug("Could not drop the stored setting '{}'", key, e);
		}
	}

	/** On the Swing thread: shows what the look-up bar should say. */
	private void showBar(LookupBarState state)
	{
		final LookupBar bar = lookupBar;
		if (bar != null)
		{
			bar.show(state);
		}
	}

	/**
	 * On the client thread: takes a look-up result. A LOADING result starts a new look-up session, added to the kept
	 * sessions and shown; any other result fills the table of the session being looked up, under the game's names, and
	 * sets its state; then the views go to the panel.
	 */
	private void lookupArrived(LookupResult result)
	{
		SessionView shownView = null;
		if (result.state() == LookupResult.State.LOADING)
		{
			lookupSession = new Session(Session.SessionKind.LOOKUP, result.title(), System.currentTimeMillis());
			lookupGeneration = result.generation();
			lookupFilled = false;
			recorder.sessions().add(lookupSession);
			shownView = lookupSession.view();
		}
		final Session session = lookupSession;
		if (session == null || result.generation() != lookupGeneration)
		{
			return;
		}
		if (result.state() != LookupResult.State.LOADING)
		{
			if (!lookupFilled)
			{
				lookupFilled = true;
				WikiNames.fill(client, modelPictures, session.table(), result.thisMonster(), Section.THIS_MONSTER, true);
				WikiNames.fill(client, modelPictures, session.table(), result.linked(), Section.LINKED, false);
			}
			else
			{
				WikiNames.pictures(client, session.table(), result.thisMonster(), Section.THIS_MONSTER);
				WikiNames.pictures(client, session.table(), result.linked(), Section.LINKED);
			}
			if (result.state() == LookupResult.State.DONE)
			{
				session.done();
			}
			else
			{
				session.fail();
			}
		}
		final List<SessionView> all = keptViews();
		final SessionView forced = shownView;
		final SceneCustomizerPanel shown = panel;
		if (shown != null)
		{
			SwingUtilities.invokeLater(() -> shown.sessionsChanged(all, forced));
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!SceneCustomizerConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if (SceneCustomizerConfig.SIDEBAR_ICON_KEY.equals(event.getKey()))
		{
			SwingUtilities.invokeLater(this::applyIcon);
			return;
		}
		if (!USE_WIKI_KEY.equals(event.getKey()))
		{
			return;
		}
		final LookupFlow looking = flow;
		final boolean wikiOn = config.useWiki();
		if (looking != null)
		{
			if (!wikiOn)
			{
				looking.stop();
			}
			looking.typed("", wikiOn);
		}
		if (!wikiOn)
		{
			clientThread.invoke(this::stopLookupSession);
		}
		final SceneCustomizerPanel shown = panel;
		if (shown != null)
		{
			SwingUtilities.invokeLater(shown::wikiChanged);
		}
	}

	/** On the client thread: the wiki was switched off; a look-up still loading is marked failed and filled no more. */
	private void stopLookupSession()
	{
		final Session session = lookupSession;
		if (session == null)
		{
			return;
		}
		lookupSession = null;
		if (session.view().state() != Session.SessionState.LOADING)
		{
			return;
		}
		session.fail();
		final List<SessionView> all = keptViews();
		final SceneCustomizerPanel shown = panel;
		if (shown != null)
		{
			SwingUtilities.invokeLater(() -> shown.sessionsChanged(all, null));
		}
	}

	/**
	 * On the client thread, at most once per game tick: when a picture landed since the last time, the views of all
	 * kept sessions go to the panel (the shown session stays).
	 */
	private void refreshPictures()
	{
		final SceneCustomizerPanel shown = panel;
		if (!picturesChanged || shown == null)
		{
			return;
		}
		picturesChanged = false;
		final List<SessionView> all = keptViews();
		SwingUtilities.invokeLater(() -> shown.sessionsChanged(all, null));
	}

	/** On the client thread: a frozen view of every kept session, newest recordings first. */
	private List<SessionView> keptViews()
	{
		final List<SessionView> views = new ArrayList<>();
		for (Session session : recorder.sessions().all())
		{
			views.add(session.view());
		}
		return views;
	}

	/**
	 * On the Swing thread: takes a wiki row out of the shown session. The work is handed to the client thread, which
	 * finds the session by its kind and start and hands the views back.
	 * Choice: only a look-up has wiki rows, so only its table is ever changed.
	 */
	private void removeRow(SessionView from, ThingKey key)
	{
		clientThread.invoke(() ->
		{
			for (Session session : recorder.sessions().all())
			{
				final SessionView view = session.view();
				if (view.kind() == from.kind() && view.startedEpochMs() == from.startedEpochMs()
					&& view.title().equals(from.title()))
				{
					session.table().remove(key);
				}
			}
			final List<SessionView> all = keptViews();
			final SceneCustomizerPanel shown = panel;
			if (shown != null)
			{
				SwingUtilities.invokeLater(() -> shown.sessionsChanged(all, null));
			}
		});
	}

	/** Opens RuneLite's colour picker on a colour, alpha hidden. */
	private void openColourPicker(Component parent, String title, Color current, Consumer<Color> onChange,
		Runnable onClose)
	{
		final RuneliteColorPicker picker = colorPickerManager.create(SwingUtilities.getWindowAncestor(parent), current,
			title, true);
		picker.setOnColorChange(onChange);
		picker.setOnClose(c -> onClose.run());
		picker.setVisible(true);
	}

	/** Puts the text on the system clipboard. */
	private static void copyToClipboard(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	/** On the client thread: the Record button's press. */
	private void toggleRecording()
	{
		final SceneCustomizerPanel shown = panel;
		if (recorder.isRecording())
		{
			recorder.stop(System.currentTimeMillis());
		}
		else
		{
			recorder.start(System.currentTimeMillis(), "Recording " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
			if (shown != null)
			{
				SwingUtilities.invokeLater(() -> shown.setRecording(true));
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		recorder.onNpcSpawned(event);
		final Engine e = engine;
		if (e != null)
		{
			e.onNpcSpawned();
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		recorder.onNpcChanged(event);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		recorder.onNpcDespawned(event);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		recorder.onAnimationChanged(event);
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		recorder.onGraphicChanged(event);
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		recorder.onGraphicsObjectCreated(event);
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		recorder.onProjectileMoved(event);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		recorder.onGameObjectSpawned(event);
		final Engine e = engine;
		if (e != null)
		{
			e.onObjectSpawned(event.getGameObject());
		}
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		recorder.onWallObjectSpawned(event);
		final Engine e = engine;
		if (e != null)
		{
			e.onObjectSpawned(event.getWallObject());
		}
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		recorder.onDecorativeObjectSpawned(event);
		final Engine e = engine;
		if (e != null)
		{
			e.onObjectSpawned(event.getDecorativeObject());
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		recorder.onGroundObjectSpawned(event);
		final Engine e = engine;
		if (e != null)
		{
			e.onObjectSpawned(event.getGroundObject());
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		objectGone(event.getGameObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		objectGone(event.getWallObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		objectGone(event.getDecorativeObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		objectGone(event.getGroundObject());
	}

	/** Tells the engine an object is gone. */
	private void objectGone(TileObject object)
	{
		final Engine e = engine;
		if (e != null)
		{
			e.onObjectDespawned(object);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		recorder.onMenuOptionClicked(event);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		recorder.onGameStateChanged(event);
		final Engine e = engine;
		if (e != null)
		{
			e.onGameStateChanged(event.getGameState());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		recorder.onGameTick(System.currentTimeMillis());
		refreshPictures();
		final Engine e = engine;
		if (e != null)
		{
			e.onGameTick();
		}
	}
}
