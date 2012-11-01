/**********************************************************************
 * $Source$
 * $Revision$
 * $Date$
 * $Author$
 *
 * Copyright (c) by Heiner Jostkleigrewe
 * This program is free software: you can redistribute it and/or modify it under the terms of the 
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See 
 *  the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, 
 * see <http://www.gnu.org/licenses/>.
 * 
 * heiner@jverein.de
 * www.jverein.de
 **********************************************************************/
package de.jost_net.JVerein.gui.view;

import de.jost_net.JVerein.Einstellungen;
import de.jost_net.JVerein.JVereinPlugin;
import de.jost_net.JVerein.gui.action.DokumentationAction;
import de.jost_net.JVerein.gui.action.StatistikDSBExportAction;
import de.jost_net.JVerein.gui.control.MitgliedControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.LabelGroup;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;

public class StatistikDSBView extends AbstractView
{

  @Override
  public void bind() throws Exception
  {
    GUI.getView().setTitle(JVereinPlugin.getI18n().tr("Statistik DSB"));

    final MitgliedControl control = new MitgliedControl(this);

    LabelGroup group = new LabelGroup(getParent(), JVereinPlugin.getI18n().tr(
        "Parameter"));
    group.addLabelPair(JVereinPlugin.getI18n().tr("Jahr"),
        control.getJubeljahr());

    ButtonArea buttons = new ButtonArea();
    buttons.addButton(JVereinPlugin.getI18n().tr("Hilfe"),
        new DokumentationAction(), DokumentationUtil.JUBILAEEN, false,
        "help-browser.png");
    Button btnStart = new Button(JVereinPlugin.getI18n().tr("Start"),
        new StatistikDSBExportAction(), control, true, "go.png");
    if (!Einstellungen.getEinstellung().getGeburtsdatumPflicht())
    {
      btnStart.setEnabled(false);
      Application.getMessagingFactory().sendMessage(
          new StatusBarMessage(
              "Einstellungen->Anzeige->Geburtsdatum: keine Pflicht. Die Statistik kann nicht erstellt werden.",
              StatusBarMessage.TYPE_ERROR));

    }
    buttons.addButton(btnStart);

    buttons.paint(getParent());
  }

  @Override
  public String getHelp()
  {
    return JVereinPlugin.getI18n().tr(
        "<form><p><span color=\"header\" font=\"header\">Jubil�en</span></p>"
            + "<p>Ausgabe der DSB-Statistik.</p></form>");
  }
}
