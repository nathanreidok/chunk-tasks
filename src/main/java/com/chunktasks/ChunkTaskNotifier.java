package com.chunktasks;

import com.chunktasks.manager.ChunkTask;
import com.chunktasks.manager.ChunkTasksManager;
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

    private WidgetNode popupWidgetNode;
    private final List<String> queuedPopups = new ArrayList<>();

    public void completeTask(ChunkTask chunkTask) {
        chunkTasksManager.completeTask(chunkTask);
        addPopupToQueue(chunkTask.name);
    }

    private void addPopupToQueue(String message) {
        String cleanMessage = message.replace("~", "").replace("|", "");
        client.addChatMessage(ChatMessageType.PUBLICCHAT, "Chunk Tasks", "Chunk Task Complete" + ": " + message, null);
        queuedPopups.add(cleanMessage);
        if (queuedPopups.size() == 1) {
            showPopup(cleanMessage);
        }
    }

    private void showPopup(String message) {
        try {
            clientThread.invokeLater(() -> {
                int componentId = client.isResized()
                        ? client.getVarbitValue(Varbits.SIDE_PANELS) == 1
                        ? ChunkTaskConstants.RESIZABLE_MODERN_LAYOUT
                        : ChunkTaskConstants.RESIZABLE_CLASSIC_LAYOUT
                        : ChunkTaskConstants.FIXED_CLASSIC_LAYOUT;

                popupWidgetNode = client.openInterface(componentId, 660, WidgetModalMode.MODAL_CLICKTHROUGH);
                client.runScript(3343, "Chunk Task Complete", message, -1);

                soundEngine.playClip(Sound.CHUNK_TASK_COMPLETE);

                clientThread.invokeLater(this::tryClearMessage);
            });
        } catch (IllegalStateException ex) {
            log.info("Client still on login page");
        }
    }

    private boolean tryClearMessage() {
        Widget w = client.getWidget(660, 1);

        if (w != null && w.getWidth() > 0) {
            return false;
        }

        client.closeInterface(popupWidgetNode, true);
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

//    private void showChunkTaskCompletePopup(String message) {
//        log.error(message);
//        log.error("popup widget node is" + (popupWidgetNode == null ? " not" : "") + " null");
//        if (popupWidgetNode != null)
//        {
//            queuedPopups.add(message);
//            return;
//        }
//
//        try {
//            clientThread.invokeLater(() -> {
//                String cleanMessage = message.replace("~", "").replace("|", "");
//                int componentId = client.isResized()
//                        ? client.getVarbitValue(Varbits.SIDE_PANELS) == 1
//                        ? ChunkTaskConstants.RESIZABLE_MODERN_LAYOUT
//                        : ChunkTaskConstants.RESIZABLE_CLASSIC_LAYOUT
//                        : ChunkTaskConstants.FIXED_CLASSIC_LAYOUT;
//
//                popupWidgetNode = client.openInterface(componentId, 660, WidgetModalMode.MODAL_CLICKTHROUGH);
//                client.runScript(3343, "Chunk Task Complete", cleanMessage, -1);
//
//                client.addChatMessage(ChatMessageType.PUBLICCHAT, "Chunk Tasks", "Chunk Task Complete" + ": " + cleanMessage, null);
//                soundEngine.playClip(Sound.CHUNK_TASK_COMPLETE);
//
//                clientThread.invokeLater(() -> {
//                    Widget w = client.getWidget(660, 1);
//
//                    if (w == null || w.getWidth() > 0) {
//                        return false;
//                    }
//
//                    client.closeInterface(popupWidgetNode, true);
//                    popupWidgetNode = null;
//                    if (!queuedPopups.isEmpty()) {
//                        clientThread.invokeLater(() -> {
//                            showChunkTaskCompletePopup(queuedPopups.remove(0));
//                            return true;
//                        });
//                    }
//                    return true;
//                });
//            });
//        } catch (IllegalStateException ex) {
//            log.info("Client still on login page");
//        }
//    }
}
