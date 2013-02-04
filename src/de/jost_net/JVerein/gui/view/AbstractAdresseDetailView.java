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

import java.rmi.RemoteException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;

import de.jost_net.JVerein.Einstellungen;
import de.jost_net.JVerein.JVereinPlugin;
import de.jost_net.JVerein.gui.action.AdresseDeleteAction;
import de.jost_net.JVerein.gui.action.AdresseDetailAction;
import de.jost_net.JVerein.gui.action.DokumentationAction;
import de.jost_net.JVerein.gui.action.KontoauszugAction;
import de.jost_net.JVerein.gui.action.MitgliedDeleteAction;
import de.jost_net.JVerein.gui.action.MitgliedDetailAction;
import de.jost_net.JVerein.gui.action.MitgliedMailSendenAction;
import de.jost_net.JVerein.gui.action.PersonalbogenAction;
import de.jost_net.JVerein.gui.control.DokumentControl;
import de.jost_net.JVerein.gui.control.MitgliedControl;
import de.jost_net.JVerein.gui.control.MitgliedskontoControl;
import de.jost_net.JVerein.gui.util.SimpleVerticalContainer;
import de.jost_net.JVerein.keys.ArtBeitragsart;
import de.jost_net.JVerein.keys.Beitragsmodel;
import de.jost_net.JVerein.rmi.Beitragsgruppe;
import de.jost_net.JVerein.rmi.Mitglied;
import de.jost_net.JVerein.rmi.MitgliedDokument;
import de.jost_net.JVerein.server.MitgliedUtils;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.input.Input;
import de.willuhn.jameica.gui.input.TextInput;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.Color;
import de.willuhn.jameica.gui.util.Container;
import de.willuhn.jameica.gui.util.LabelGroup;
import de.willuhn.jameica.gui.util.ScrolledContainer;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.gui.util.TabGroup;

public abstract class AbstractAdresseDetailView extends AbstractView
{

  // Statische Variable, die den zuletzt ausgew�hlten Tab speichert.
  private static int tabindex = -1;

  final MitgliedControl control = new MitgliedControl(this);

  /**
   * @wbp.parser.entryPoint
   */
  @Override
  public void bind() throws Exception
  {
    GUI.getView().setTitle(JVereinPlugin.getI18n().tr(getTitle()));

    final MitgliedskontoControl controlMk = new MitgliedskontoControl(this);

    ScrolledContainer scrolled = new ScrolledContainer(getParent(), 1);

    SimpleContainer oben = new SimpleContainer(scrolled.getComposite(), false,
        1);

    final TabFolder folder = new TabFolder(scrolled.getComposite(), SWT.NONE);
    folder.setLayoutData(new GridData(GridData.FILL_BOTH));
    folder.setBackground(Color.BACKGROUND.getSWTColor());

    DBIterator zhl = Einstellungen.getDBService().createList(Mitglied.class);
    MitgliedUtils.setNurAktive(zhl);
    MitgliedUtils.setMitglied(zhl);
    zhl.setOrder("ORDER BY name, vorname");

    int anzahlSpalten = Einstellungen.getEinstellung()
        .getAnzahlSpaltenStammdaten();
    boolean showInTab = Einstellungen.getEinstellung()
        .getZeigeStammdatenInTab();
    zeicheStammdaten(showInTab ? folder : oben.getComposite(), anzahlSpalten);

    anzahlSpalten = Einstellungen.getEinstellung()
        .getAnzahlSpaltenMitgliedschaft();
    showInTab = Einstellungen.getEinstellung().getZeigeMitgliedschaftInTab();
    zeichneMitgliedschaft(showInTab ? folder : oben.getComposite(),
        anzahlSpalten);

    anzahlSpalten = Einstellungen.getEinstellung().getAnzahlSpaltenZahlung();
    showInTab = Einstellungen.getEinstellung().getZeigeZahlungInTab();
    zeichneZahlung(showInTab ? folder : oben.getComposite(), anzahlSpalten);

    showInTab = Einstellungen.getEinstellung().getZeigeZusatzbetraegeInTab();
    zeichneZusatzbeitraege(showInTab ? folder : oben.getComposite());

    showInTab = Einstellungen.getEinstellung().getZeigeMitgliedskontoInTab();
    zeichneMitgliedkonto(controlMk, showInTab ? folder : oben.getComposite());

    showInTab = Einstellungen.getEinstellung().getZeigeVermerkeInTab();
    zeichneVermerke(showInTab ? folder : oben.getComposite(), 1);

    showInTab = Einstellungen.getEinstellung().getZeigeWiedervorlageInTab();
    zeichneWiedervorlage(showInTab ? folder : oben.getComposite());

    showInTab = Einstellungen.getEinstellung().getZeigeMailsInTab();
    zeichneMails(showInTab ? folder : oben.getComposite());

    showInTab = Einstellungen.getEinstellung().getZeigeEigenschaftenInTab();
    zeichneEigenschaften(showInTab ? folder : oben.getComposite());

    anzahlSpalten = Einstellungen.getEinstellung()
        .getAnzahlSpaltenZusatzfelder();
    showInTab = Einstellungen.getEinstellung().getZeigeZusatzfelderInTab();
    zeichneZusatzfelder(showInTab ? folder : oben.getComposite(), anzahlSpalten);

    showInTab = Einstellungen.getEinstellung().getZeigeLehrgaengeInTab();
    zeichneLehrgaenge(showInTab ? folder : oben.getComposite());

    showInTab = Einstellungen.getEinstellung().getZeigeFotoInTab();
    zeichneMitgliedfoto(showInTab ? folder : oben.getComposite());

    anzahlSpalten = Einstellungen.getEinstellung().getAnzahlSpaltenLesefelder();
    showInTab = Einstellungen.getEinstellung().getZeigeLesefelderInTab();
    zeichneLesefelder(showInTab ? folder : oben.getComposite(), anzahlSpalten);

    zeichneMitgliedDetail(showInTab ? folder : oben.getComposite());

    zeichneDokumente(showInTab ? folder : oben.getComposite());

    // Aktivier zuletzt ausgew�hlten Tab.
    if (tabindex != -1)
    {
      folder.setSelection(tabindex);
    }
    folder.addSelectionListener(new SelectionListener()
    {

      // Wenn Tab angeklickt, speicher diesen um ihn sp�ter automatisch
      // wieder auszuw�hlen.
      @Override
      public void widgetSelected(SelectionEvent arg0)
      {
        tabindex = folder.getSelectionIndex();
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent arg0)
      {
      }
    });

    zeichneButtonArea(getParent());

  }

  private void zeichneButtonArea(Composite parent) throws RemoteException
  {
    ButtonArea buttons = new ButtonArea();
    if (!control.getMitglied().isNewObject()
        && Einstellungen.getEinstellung().getMitgliedskonto())
    {
      MitgliedskontoControl mkcontrol = new MitgliedskontoControl(this);
      mkcontrol.getStartKontoauszugButton(control.getMitglied(), null, null);
      buttons
          .addButton(new Button(JVereinPlugin.getI18n().tr("Kontoauszug"),
              new KontoauszugAction(), control.getMitglied(), false,
              "rechnung.png"));
    }
    if (isMitgliedDetail())
    {
      buttons.addButton(new Button(JVereinPlugin.getI18n().tr("Personalbogen"),
          new PersonalbogenAction(), control.getCurrentObject(), false,
          "rechnung.png"));
    }
    buttons.addButton(JVereinPlugin.getI18n().tr("Hilfe"),
        new DokumentationAction(), DokumentationUtil.MITGLIED, false,
        "help-browser.png");
    buttons.addButton(JVereinPlugin.getI18n().tr("Mail"),
        new MitgliedMailSendenAction(), getCurrentObject(), false,
        "mail-message-new.png");
    buttons.addButton(JVereinPlugin.getI18n().tr("neu"),
        isMitgliedDetail() ? new MitgliedDetailAction()
            : new AdresseDetailAction(), null, false, "document-new.png");
    buttons.addButton(JVereinPlugin.getI18n().tr("l�schen"),
        (isMitgliedDetail() ? new MitgliedDeleteAction()
            : new AdresseDeleteAction()), control.getCurrentObject(), false,
        "user-trash.png");
    buttons.addButton(JVereinPlugin.getI18n().tr("speichern"), new Action()
    {

      @Override
      public void handleAction(Object context)
      {
        control.handleStore();
      }
    }, null, true, "document-save.png");
    buttons.paint(parent);
  }

  private void zeichneDokumente(Composite parentComposite)
      throws RemoteException
  {
    if (JVereinPlugin.isArchiveServiceActive()
        && !control.getMitglied().isNewObject())
    {
      Container cont = getTabOrLabelContainer(parentComposite, JVereinPlugin
          .getI18n().tr("Dokumente"));

      MitgliedDokument mido = (MitgliedDokument) Einstellungen.getDBService()
          .createObject(MitgliedDokument.class, null);
      mido.setReferenz(new Integer(control.getMitglied().getID()));
      DokumentControl dcontrol = new DokumentControl(this, "mitglieder", true);
      cont.addPart(dcontrol.getDokumenteList(mido));
      ButtonArea butts = new ButtonArea();
      butts.addButton(dcontrol.getNeuButton(mido));
      butts.paint(cont.getComposite());
    }
  }

  private void zeichneMitgliedDetail(Composite parentComposite)
      throws RemoteException
  {
    if (isMitgliedDetail()
        && Einstellungen.getEinstellung().getArbeitseinsatz())
    {
      Container cont = getTabOrLabelContainer(parentComposite, JVereinPlugin
          .getI18n().tr("Arbeitseinsatz"));

      ButtonArea buttonsarbeins = new ButtonArea();
      buttonsarbeins.addButton(control.getArbeitseinsatzNeu());
      buttonsarbeins.paint(cont.getComposite());

      cont.getComposite().setLayoutData(new GridData(GridData.FILL_VERTICAL));
      cont.getComposite().setLayout(new GridLayout(1, false));

      control.getArbeitseinsatzTable().paint(cont.getComposite());
    }
  }

  private void zeichneLesefelder(Composite parentComposite, int spaltenanzahl)
      throws RemoteException
  {
    // TODO: getLesefelder() ist zu langsam. Inhalt von Lesefeldern sollte erst
    // evaluiert werden, wenn Lesefelder-Tab angeklickt wird.
    if (Einstellungen.getEinstellung().getUseLesefelder())
    {
      Input[] lesefelder = control.getLesefelder();
      if (lesefelder != null)
      {
        Container cont = getTabOrLabelContainer(parentComposite, JVereinPlugin
            .getI18n().tr("Lesefelder"));
        SimpleVerticalContainer svc = new SimpleVerticalContainer(
            cont.getComposite(), false, spaltenanzahl);
        for (Input inp : lesefelder)
        {
          if (inp == null)
          {
            String errorText = JVereinPlugin
                .getI18n()
                .tr("Achtung! Ung�ltiges Lesefeld-Skript gefunden. Diesen Fehler bitte unter www.jverein.de/forum melden!");
            Input errorInput = new TextInput(errorText);
            errorInput.setEnabled(false);
            svc.addInput(errorInput);
            GUI.getStatusBar().setErrorText(errorText);
          }
          else
          {
            svc.addInput(inp);
          }
        }
        svc.arrangeVertically();
        ButtonArea buttonszus = new ButtonArea();
        buttonszus.addButton(control.getLesefelderEdit());
        cont.addButtonArea(buttonszus);
      }
    }
  }

  private void zeichneMitgliedfoto(Composite parentComposite)
      throws RemoteException
  {
    if (isMitgliedDetail() && Einstellungen.getEinstellung().getMitgliedfoto())
    {
      Container cont = getTabOrLabelContainer(parentComposite, JVereinPlugin
          .getI18n().tr("Foto"));
      cont.addLabelPair(JVereinPlugin.getI18n().tr("Foto"), control.getFoto());
    }
  }

  private void zeichneLehrgaenge(Composite parentComposite)
      throws RemoteException
  {
    if (isMitgliedDetail() && Einstellungen.getEinstellung().getLehrgaenge())
    {
      Container cont = getTabOrLabelContainer(parentComposite, JVereinPlugin
          .getI18n().tr("Lehrg�nge"));

      cont.getComposite().setLayoutData(new GridData(GridData.FILL_VERTICAL));
      cont.getComposite().setLayout(new GridLayout(1, false));

      ButtonArea buttonslehrg = new ButtonArea();
      buttonslehrg.addButton(control.getLehrgangNeu());
      buttonslehrg.paint(cont.getComposite());
      control.getLehrgaengeTable().paint(cont.getComposite());
    }
  }

  private void zeichneZusatzfelder(Composite parentComposite, int spaltenanzahl)
      throws RemoteException
  {
    Input[] zusatzfelder = control.getZusatzfelder();
    if (zusatzfelder != null)
    {
      Container cont = getTabOrLabelContainer(parentComposite, JVereinPlugin
          .getI18n().tr("Zusatzfelder"));
      SimpleVerticalContainer svc = new SimpleVerticalContainer(
          cont.getComposite(), true, spaltenanzahl);
      for (Input inp : zusatzfelder)
      {
        svc.addInput(inp);
      }
      svc.arrangeVertically();
    }
  }

  private void zeichneEigenschaften(Composite parentComposite)
      throws RemoteException
  {
    // if (isMitgliedDetail())
    // {
    Container cont = getTabOrLabelContainer(parentComposite, JVereinPlugin
        .getI18n().tr("Eigenschaften"));

    control.getEigenschaftenTree().paint(cont.getComposite());
    // }
  }

  private void zeichneWiedervorlage(Composite parentComposite)
      throws RemoteException
  {
    if (Einstellungen.getEinstellung().getWiedervorlage())
    {
      Container cont = getTabOrLabelContainer(parentComposite, JVereinPlugin
          .getI18n().tr("Wiedervorlage"));

      cont.getComposite().setLayoutData(new GridData(GridData.FILL_VERTICAL));
      cont.getComposite().setLayout(new GridLayout(1, false));

      ButtonArea buttonswvl = new ButtonArea();
      buttonswvl.addButton(control.getWiedervorlageNeu());
      buttonswvl.paint(cont.getComposite());
      control.getWiedervorlageTable().paint(cont.getComposite());
    }
  }

  private void zeichneMails(Composite parentComposite) throws RemoteException
  {
    if (Einstellungen.getEinstellung().getSmtpServer() != null
        && Einstellungen.getEinstellung().getSmtpServer().length() > 0)
    {
      Container cont = getTabOrLabelContainer(parentComposite, JVereinPlugin
          .getI18n().tr("Mails"));

      control.getMailTable().paint(cont.getComposite());
    }
  }

  private void zeichneVermerke(Composite parentComposite, int spaltenanzahl)
      throws RemoteException
  {
    if (Einstellungen.getEinstellung().getVermerke())
    {
      Container cont = getTabOrLabelContainer(parentComposite, JVereinPlugin
          .getI18n().tr("Vermerke"));
      SimpleContainer cols = new SimpleContainer(cont.getComposite(), true,
          spaltenanzahl * 2);

      // Stelle sicher, dass Eingabefeld sich �ber mehrere Zeilen erstreckt.
      GridData gridData = new GridData(GridData.FILL_BOTH);
      gridData.minimumHeight = 80;
      cols.getComposite().setLayoutData(gridData);

      cols.addLabelPair(JVereinPlugin.getI18n().tr("Vermerk 1"),
          control.getVermerk1());
      cols.addLabelPair(JVereinPlugin.getI18n().tr("Vermerk 2"),
          control.getVermerk2());
    }
  }

  /**
   * Zeichnet das Mitgliedskonto, wenn dieses aktiviert ist und es sich nicht um
   * ein neues Mitglied handelt (f�r dieses macht ein Mitgliedskonto noch kein
   * Sinn!)
   * 
   * @param parentComposite
   * @param spaltenanzahl
   * @throws RemoteException
   */
  private void zeichneMitgliedkonto(MitgliedskontoControl controlMk,
      Composite parentComposite) throws RemoteException
  {
    if (!control.getMitglied().isNewObject()
        && Einstellungen.getEinstellung().getMitgliedskonto())
    {
      Container cont = getTabOrLabelContainer(parentComposite, JVereinPlugin
          .getI18n().tr("Mitgliedskonto"));
      controlMk.getMitgliedskontoTree(control.getMitglied()).paint(
          cont.getComposite());
    }
  }

  private void zeichneZusatzbeitraege(Composite parentComposite)
      throws RemoteException
  {
    if (isMitgliedDetail() && Einstellungen.getEinstellung().getZusatzbetrag())
    {
      Container cont = getTabOrLabelContainer(parentComposite, JVereinPlugin
          .getI18n().tr("Zusatzbetr�ge"));

      cont.getComposite().setLayoutData(new GridData(GridData.FILL_VERTICAL));
      cont.getComposite().setLayout(new GridLayout(1, false));

      ButtonArea buttonszus = new ButtonArea();
      buttonszus.addButton(control.getZusatzbetragNeu());
      buttonszus.paint(cont.getComposite());
      control.getZusatzbetraegeTable().paint(cont.getComposite());
    }
  }

  private void zeichneZahlung(Composite parentComposite, int spaltenanzahl)
      throws RemoteException
  {
    Container container = getTabOrLabelContainer(parentComposite, JVereinPlugin
        .getI18n().tr("Zahlung"));
    SimpleVerticalContainer cols = new SimpleVerticalContainer(
        container.getComposite(), true, spaltenanzahl);

    if (isMitgliedDetail())
    {
      cols.addInput(control.getZahlungsweg());
      if (Einstellungen.getEinstellung().getBeitragsmodel() == Beitragsmodel.MONATLICH12631)
      {
        cols.addInput(control.getZahlungsrhytmus());
      }
    }
    cols.addInput(control.getKontoinhaber());
    cols.addInput(control.getBic());
    cols.addInput(control.getIban());
    cols.addInput(control.getBlz());
    cols.addInput(control.getKonto());

    cols.arrangeVertically();
  }

  /**
   * Erzeugt einen Container, der in einem TabFolder oder einer LabelGroup
   * eingebettet ist. Ist parentComposite ein TabFolder wird SimpleContainer in
   * eine TabGroup eingebettet, anderenfalls in eine LabelGroup.
   * 
   * @param parentComposite
   *          Parent composite in das TabGroup bzw. LabelGroup und
   *          SimpleContainer gezeichnet wird.
   * @param titel
   *          Beschriftung von TabGroup bzw. LabelGroup
   * @return SimpleContainer, in den Inhalt gezeichnet werden kann.
   */
  private Container getTabOrLabelContainer(Composite parentComposite,
      String titel)
  {
    Container container;
    if (parentComposite instanceof TabFolder)
    {
      container = new TabGroup((TabFolder) parentComposite, titel);
    }
    else
    {
      container = new LabelGroup(parentComposite, titel);
    }
    return container;
  }

  private void zeichneMitgliedschaft(Composite parentComposite,
      int spaltenanzahl) throws RemoteException
  {
    if (isMitgliedDetail())
    {
      Container container = getTabOrLabelContainer(parentComposite,
          JVereinPlugin.getI18n().tr("Mitgliedschaft"));

      SimpleVerticalContainer cols = new SimpleVerticalContainer(
          container.getComposite(), false, spaltenanzahl);

      if (Einstellungen.getEinstellung().getExterneMitgliedsnummer())
      {
        cols.addInput(control.getExterneMitgliedsnummer());
      }
      else
      {
        cols.addInput(control.getMitgliedsnummer());
      }
      cols.addInput(control.getEintritt());
      cols.addInput(control.getAustritt());
      cols.addInput(control.getBeitragsgruppe(true));
      if (Einstellungen.getEinstellung().getIndividuelleBeitraege())
      {
        cols.addInput(control.getIndividuellerBeitrag());
      }
      cols.addInput(control.getKuendigung());
      if (Einstellungen.getEinstellung().getSterbedatum()
          && control.getMitglied().getPersonenart().equals("n"))
      {
        cols.addInput(control.getSterbetag());
      }
      cols.arrangeVertically();

      // Wenn es mindestens eine Beitragsgruppe mit Beitragsart
      // "Familie: Zahler"
      // oder "Familie: Angeh�riger" gibt, zeige Familienverband-Part.
      // Dieser Familien-Part soll �ber die komplette Breite angezeigt werden,
      // kann daher nicht im SimpleVerticalContainer angezeigt werden.
      DBIterator it = Einstellungen.getDBService().createList(
          Beitragsgruppe.class);
      it.addFilter("beitragsart = ? or beitragsart = ?",
          ArtBeitragsart.FAMILIE_ZAHLER, ArtBeitragsart.FAMILIE_ANGEHOERIGER);
      if (it.hasNext())
      {
        // Verstecke Familienverband wenn aktuelles Mitglied nicht Teil einer
        // Familie ist.
        if (control.getMitglied().getBeitragsgruppe() != null
            && control.getMitglied().getBeitragsgruppe().getBeitragsArt() == ArtBeitragsart.NORMAL)
        {
          control.getFamilienverband().setVisible(false);
        }
        // Container l�sst nur das Hinzuf�gen von Parts zu.
        // Aus diesem Grund ist Part Familienverband dynamisch:
        // Entweder wird der Familienverband angezeigt (setShow(true))
        // oder ein leeres Composite (setShow(false))
        container.addPart(control.getFamilienverband());
      }
    }
  }

  /**
   * Zeichnet GUI-Felder f�r Stammdaten. Wenn Kommunikationsdaten aktiviert
   * sind, werden drei Spalten angezeigt, ansonsten zwei.
   * 
   * @param parentComposite
   *          Composite auf dem gezeichnet wird.
   * @throws RemoteException
   */
  private void zeicheStammdaten(Composite parentComposite, int spaltenanzahl)
      throws RemoteException
  {
    Container container = getTabOrLabelContainer(parentComposite, JVereinPlugin
        .getI18n().tr("Stammdaten"));
    SimpleVerticalContainer cols = new SimpleVerticalContainer(
        container.getComposite(), true, spaltenanzahl);

    if (!isMitgliedDetail())
    {
      cols.addInput(control.getAdresstyp());
    }
    cols.addInput(control.getAnrede());
    if (control.getMitglied().getPersonenart().equals("n"))
    {
      cols.addInput(control.getTitel());
    }
    if (control.getMitglied().getPersonenart().equals("j"))
    {
      control.getName(true).setName(JVereinPlugin.getI18n().tr("Name Zeile 1"));
      control.getVorname().setName(JVereinPlugin.getI18n().tr("Name Zeile 2"));
      control.getVorname().setMandatory(false);
    }
    cols.addInput(control.getName(true));
    cols.addInput(control.getVorname());
    cols.addInput(control.getAdressierungszusatz());

    cols.addInput(control.getStrasse());
    cols.addInput(control.getPlz());
    cols.addInput(control.getOrt());
    if (Einstellungen.getEinstellung().getAuslandsadressen())
    {
      cols.addInput(control.getStaat());
    }
    if (control.getMitglied().getPersonenart().equals("n"))
    {
      cols.addInput(control.getGeburtsdatum());
      cols.addInput(control.getGeschlecht());
    }

    if (Einstellungen.getEinstellung().getKommunikationsdaten())
    {
      cols.addInput(control.getTelefonprivat());
      cols.addInput(control.getHandy());
      cols.addInput(control.getTelefondienstlich());
      cols.addInput(control.getEmail());
    }
    cols.arrangeVertically();
  }

  @Override
  public String getHelp()
  {
    return JVereinPlugin
        .getI18n()
        .tr("<form><p><span color=\"header\" font=\"header\">Mitglied</span></p>"
            + "<li>Anrede: Herrn, Frau ...</li>"
            + "<li>Titel: Dr., Prof. Dr., ...</li>"
            + "<li>Sofern Auslandsadressen erfasst werden sollen, ist das unter Einstellungen anzuhaken. Dann kann auch der Wohnungsstaat eingegeben werden.</li>"
            + "<li>Adressierungszusatz: z. B. bei Lieschen M�ller</li>"
            + "<li>Kontoinhaber: Falls das Mitglied nicht Kontoinhaber ist, k�nnen die entsprechenden Daten eingegeben werden.</li>"
            + "<li>Austritt: Das laut Satzung g�ltige Austrittsdatum.</li>"
            + "<li>K�ndigung: Eingangsdatum der K�ndigung.</li>"
            + "<li>Zusatzabbuchung: Sind f�r das Mitglied zus�tzlich zum Jahresbeitrag weitere Betr�ge abzubuchen, k�nnen die entsprechenden Eingaben gemacht werden.</li>"
            + "</form>");
  }

  public abstract String getTitle();

  public abstract boolean isMitgliedDetail();

}
