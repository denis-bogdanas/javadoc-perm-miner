package edu.oregonstate.jdminer.settings;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * @author Nicholas Nelson <nelsonni@oregonstate.edu> Created on on 8/3/16.
 */
public class NotificationsManager {

    private final Project project;

    private NotificationsManager(Project project) {
        this.project = project;
    }

    public static NotificationsManager getInstance(Project project) {
        return new NotificationsManager(project);
    }

    public void stateErrorNotice(@NotNull String message) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getSouthEastOf(statusBar.getComponent()), Balloon.Position.atRight);
    }

    public void containingClassNotice(@NotNull PsiClass aClass) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        String message = "Containing Class: " + (aClass.getName() == null ? "No Class Name" : aClass.getName());
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getSouthEastOf(statusBar.getComponent()), Balloon.Position.atRight);
    }

    public void nullMethodNotice() {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        String message = "Selection must be inside of a method";
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, MessageType.INFO, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getSouthEastOf(statusBar.getComponent()), Balloon.Position.atRight);
    }

    public void returnTypeNotice(@NotNull Editor editor, PsiMethod method) {
        VisualPosition position = editor.offsetToVisualPosition(method.getTextOffset());
        final Point point = editor.visualPositionToXY(position);
        String message = "Cannot apply conversion to method '" + method.getName() + "'. Return types not supported.";
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, MessageType.WARNING, null)
                .setFadeoutTime(7500)
                .setHideOnKeyOutside(true)
                .createBalloon()
                .show(new RelativePoint(editor.getContentComponent(), point), Balloon.Position.above);
    }

    public void anonymousClassNotice(@NotNull Editor editor, PsiMethod method, SelectionModel selectionModel) {
        VisualPosition position = editor.offsetToVisualPosition(selectionModel.getSelectionStart());
        final Point point = editor.visualPositionToXY(position);
        String message = "Cannot apply conversion to method '" + method.getName() +
                "'. Variable references within anonymous inner class are not supported.";
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, MessageType.WARNING, null)
                .setFadeoutTime(7500)
                .setHideOnKeyOutside(true)
                .createBalloon()
                .show(new RelativePoint(editor.getContentComponent(), point), Balloon.Position.above);
    }

    public void templateLoadError(String templateName) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        String message = "DroidPermPlugin: Template '" + templateName + "' could not be loaded.";
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, MessageType.ERROR, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getSouthEastOf(statusBar.getComponent()), Balloon.Position.atRight);
    }
}
