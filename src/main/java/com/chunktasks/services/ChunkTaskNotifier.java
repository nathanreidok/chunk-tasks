package com.chunktasks.services;

import com.chunktasks.ChunkTaskConstants;
import com.chunktasks.ChunkTasksConfig;
import com.chunktasks.tasks.ChunkTask;
import com.chunktasks.managers.ChunkTasksManager;
import com.chunktasks.panel.ChunkTasksPanel;
import com.chunktasks.sound.Sound;
import com.chunktasks.sound.SoundEngine;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.WidgetNode;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChunkTaskNotifier {
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ChunkTasksManager chunkTasksManager;
    @Inject private ChunkTasksPanel panel;
    @Inject private SoundEngine soundEngine;
    @Inject private ChunkTasksConfig config;

    private WidgetNode popupWidgetNode;
    private final List<String> queuedPopups = new ArrayList<>();

    public void completeTask(ChunkTask chunkTask) {
        completeTask(chunkTask, true);
    }
    public void completeTask(ChunkTask chunkTask, boolean showPopup) {
        chunkTasksManager.completeTask(chunkTask);
        addNotificationToQueue(chunkTask.name, showPopup);
    }

    private void addNotificationToQueue(String message, boolean showPopup) {
        String cleanMessage = message.replace("~", "").replace("|", "");
        client.addChatMessage(ChatMessageType.PUBLICCHAT, "Chunk Tasks", "Chunk Task Complete" + ": " + cleanMessage, null);
        if (!showPopup)
            return;
        queuedPopups.add(cleanMessage);
        if (queuedPopups.size() == 1) {
            showPopup(cleanMessage);
        }
    }

    private void showPopup(String message) {
        clientThread.invokeLater(() -> {
            try {
                int componentId = client.isResized()
                        ? client.getVarbitValue(Varbits.SIDE_PANELS) == 1
                        ? ChunkTaskConstants.RESIZABLE_MODERN_LAYOUT
                        : ChunkTaskConstants.RESIZABLE_CLASSIC_LAYOUT
                        : ChunkTaskConstants.FIXED_CLASSIC_LAYOUT;

                popupWidgetNode = client.openInterface(componentId, 660, WidgetModalMode.MODAL_CLICKTHROUGH);
                client.runScript(3343, "Chunk Task Complete", message, -1);

                int gameVolume = client.getPreferences().getSoundEffectVolume();

                if (config.taskCompletedSound() != Sound.NONE) {
                    soundEngine.playClip(config.taskCompletedSound(), gameVolume);
                }

                clientThread.invokeLater(this::tryClearMessage);
            } catch (IllegalStateException ex) {
                log.debug("Failed to show popup");
                clientThread.invokeLater(this::tryClearMessage);
            }
        });
    }

    private boolean tryClearMessage() {
        Widget w = client.getWidget(660, 1);

        if (w != null && w.getWidth() > 0) {
            return false;
        }

        try {
            client.closeInterface(popupWidgetNode, true);
        } catch (Exception ex) {
            log.debug("Failed to clear message");
        }
        popupWidgetNode = null;
        queuedPopups.remove(0);

        if (!queuedPopups.isEmpty()) {
            clientThread.invokeLater(() -> {
                showPopup(queuedPopups.get(0));
                return true;
            });
        }
        return true;
    }
}
