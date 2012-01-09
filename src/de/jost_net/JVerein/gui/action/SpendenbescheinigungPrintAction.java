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
package de.jost_net.JVerein.gui.action;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import jonelo.NumericalChameleon.SpokenNumbers.GermanNumber;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;

import de.jost_net.JVerein.Einstellungen;
import de.jost_net.JVerein.JVereinPlugin;
import de.jost_net.JVerein.Variable.AllgemeineMap;
import de.jost_net.JVerein.io.FormularAufbereitung;
import de.jost_net.JVerein.io.Reporter;
import de.jost_net.JVerein.keys.HerkunftSpende;
import de.jost_net.JVerein.keys.Spendenart;
import de.jost_net.JVerein.rmi.Buchung;
import de.jost_net.JVerein.rmi.Formular;
import de.jost_net.JVerein.rmi.Spendenbescheinigung;
import de.jost_net.JVerein.util.Dateiname;
import de.jost_net.JVerein.util.JVDateFormatTTMMJJJJ;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/**
 * Action zur Generierung von Spendenbescheinigungen aus der Datenbank.<br>
 * Diese Klasse kapselt die Generierung des Standard-Formulars und wird auch bei
 * der Generierung eines Dokuments aus der Detailansicht der
 * Spendenbescheinigung heraus verwendet.
 */
public class SpendenbescheinigungPrintAction implements Action
{
  private boolean standardPdf = true;

  private String fileName = null;

  private de.willuhn.jameica.system.Settings settings;

  /**
   * Konstruktor ohne Parameter. Es wird angenommen, dass das Standard-Dokument
   * aufbereitet werden soll.
   */
  public SpendenbescheinigungPrintAction()
  {
    super();
    settings = new de.willuhn.jameica.system.Settings(this.getClass());
    settings.setStoreWhenRead(true);
  }

  /**
   * Konstruktor. �ber den Parameter kann festgelegt werden, ob das Standard-
   * oder das individuelle Dokument aufbereitet werden soll.
   * 
   * @param standard
   *          true=Standard-Dokument, false=individuelles Dokument
   */
  public SpendenbescheinigungPrintAction(boolean standard)
  {
    super();
    settings = new de.willuhn.jameica.system.Settings(this.getClass());
    settings.setStoreWhenRead(true);
    standardPdf = standard;
  }

  /**
   * Konstruktor. �ber den Parameter kann festgelegt werden, ob das Standard-
   * oder das individuelle Dokument aufbereitet werden soll.
   * 
   * @param standard
   *          true=Standard-Dokument, false=individuelles Dokument
   * @param fileName
   *          Dateiname als Vorgabe inklusive Pfad
   */
  public SpendenbescheinigungPrintAction(boolean standard, String fileName)
  {
    super();
    settings = new de.willuhn.jameica.system.Settings(this.getClass());
    settings.setStoreWhenRead(true);
    standardPdf = standard;
    this.fileName = fileName;
  }

  /**
   * Aufbereitung der Spendenbescheinigungen<br>
   * Hinweis: Das bzw. die generierten Formulare werden nicht im Acrobat Reader
   * angezeigt.
   * 
   * @param context
   *          Die Spendenbescheinigung(en)
   */
  public void handleAction(Object context) throws ApplicationException
  {
    Spendenbescheinigung[] spbArr = null;
    // Pr�fung des Contexs, vorhanden, eine oder mehrere
    if (context instanceof TablePart)
    {
      TablePart tp = (TablePart) context;
      context = tp.getSelection();
    }
    if (context == null)
    {
      throw new ApplicationException(JVereinPlugin.getI18n().tr(
          "Keine Spendenbescheinigung ausgew�hlt"));
    }
    else if (context instanceof Spendenbescheinigung)
    {
      spbArr = new Spendenbescheinigung[] { (Spendenbescheinigung) context };
    }
    else if (context instanceof Spendenbescheinigung[])
    {
      spbArr = (Spendenbescheinigung[]) context;
    }
    else
    {
      return;
    }
    // Aufbereitung
    try
    {
      String path = Einstellungen.getEinstellung()
          .getSpendenbescheinigungverzeichnis();
      if (path == null)
      {
        path = settings.getString("lastdir", System.getProperty("user.home"));
      }
      else if (path.length() == 0)
      {
        path = settings.getString("lastdir", System.getProperty("user.home"));
      }

      path = path.endsWith("\\") ? path : path + "\\";
      settings.setAttribute("lastdir", path);
      if (!standardPdf)
      {
        // Check ob auch f�r alle Spendenbescheinigungen ein Forumular
        // ausgewaehlt ist
        for (Spendenbescheinigung spb : spbArr)
        {
          Formular spendeformular = spb.getFormular();
          if (spendeformular == null)
          {
            GUI.getStatusBar()
                .setErrorText(
                    "Nicht alle Spendenbescheinigungen haben ein g�ltiges Formular!");
            return;
          }
        }
      }
      // Start der Aufbereitung der Dokumente
      for (Spendenbescheinigung spb : spbArr)
      {
        String fileName = null;
        if (spbArr.length > 1 || this.fileName == null)
        {
          // Dokumentennamen aus konfiguriertem Verzeichnis und dem
          // DateinamenmusterSpende
          // zusammensetzen, wenn mehr als eine Spendenbescheinigung
          // aufzubereiten
          // oder keine Vorgabe f�r einen Dateinamen gemacht wurde.
          if (spb.getMitglied() != null)
          {
            fileName = new Dateiname(spb.getMitglied(),
                spb.getBescheinigungsdatum(), "Spendenbescheinigung",
                Einstellungen.getEinstellung().getDateinamenmusterSpende(),
                "pdf").get();
          }
          else
          {
            fileName = new Dateiname(spb.getZeile1(), spb.getZeile2(),
                spb.getBescheinigungsdatum(), "Spendenbescheinigung",
                Einstellungen.getEinstellung().getDateinamenmusterSpende(),
                "pdf").get();
          }
          fileName = path + fileName;
        }
        else
        {
          fileName = this.fileName;
        }
        final File file = new File(fileName);
        // Aufbereitung des Dokumentes
        if (standardPdf)
        {
          generiereSpendenbescheinigungStandard(spb, fileName);
        }
        else
        {
          Formular fo = (Formular) Einstellungen.getDBService().createObject(
              Formular.class, spb.getFormular().getID());
          Map<String, Object> map = spb.getMap(null);
          map = new AllgemeineMap().getMap(map);
          FormularAufbereitung fa = new FormularAufbereitung(file);
          fa.writeForm(fo, map);
          fa.closeFormular();
        }
      }
    }
    catch (FileNotFoundException ie)
    {
      String fehler = JVereinPlugin.getI18n().tr(
          "Fehler beim Aufbereiten der Spendenbescheinigung");
      GUI.getStatusBar().setErrorText(fehler);
      Logger.error(fehler, ie);
    }
    catch (IOException ie)
    {
      String fehler = JVereinPlugin.getI18n().tr(
          "Fehler beim Aufbereiten der Spendenbescheinigung");
      GUI.getStatusBar().setErrorText(fehler);
      Logger.error(fehler, ie);
    }
    catch (DocumentException ie)
    {
      String fehler = JVereinPlugin.getI18n().tr(
          "Fehler beim Aufbereiten der Spendenbescheinigung");
      GUI.getStatusBar().setErrorText(fehler);
      Logger.error(fehler, ie);
    }
  }

  /**
   * Generierung des Standard-Dokumentes
   * 
   * @param spb
   *          Die Spendenbescheinigung aus der Datenbank
   * @param fileName
   *          Der Dateiname, wohin das Dokument geschrieben werden soll
   * @throws IOException
   * @throws DocumentException
   */
  private void generiereSpendenbescheinigungStandard(Spendenbescheinigung spb,
      String fileName) throws IOException, DocumentException
  {
    final File file = new File(fileName);
    FileOutputStream fos = new FileOutputStream(file);

    boolean isSammelbestaetigung = spb.isSammelbestaetigung();

    Reporter rpt = new Reporter(fos, 80, 50, 50, 50);
    rpt.addHeaderColumn(
        "Aussteller (Bezeichnung und Anschrift der steuerbeg�nstigten Einrichtung)",
        Element.ALIGN_CENTER, 100, Color.LIGHT_GRAY);

    rpt.createHeader();

    rpt.addColumn("\n" + getAussteller() + "\n ", Element.ALIGN_LEFT);
    rpt.closeTable();

    switch (spb.getSpendenart())
    {
      case Spendenart.GELDSPENDE:

        String bestaetigungsart = "Best�tigung";
        if (isSammelbestaetigung)
        {
          bestaetigungsart = "Sammelbest�tigung";
        }

        rpt.add(
            bestaetigungsart
                + " �ber Geldzuwendungen"
                + (Einstellungen.getEinstellung().getMitgliedsbetraege() ? "/Mitgliedsbeitrag"
                    : ""), 13);
        break;
      case Spendenart.SACHSPENDE:
        rpt.add("Best�tigung �ber Sachzuwendungen", 13);
        break;
    }
    rpt.add(
        "im Sinne des � 10b des Einkommenssteuergesetzes an eine der in � 5 Abs. 1 Nr. 9 des K�rperschaftssteuergesetzes "
            + "bezeichneten K�rperschaften, Personenvereinigungen oder Verm�gensmassen\n",
        10);

    rpt.addHeaderColumn("Name und Anschrift des Zuwendenden",
        Element.ALIGN_CENTER, 100, Color.LIGHT_GRAY);
    rpt.createHeader();
    String zuwendender = spb.getZeile1() + "\n" + spb.getZeile2() + "\n"
        + spb.getZeile3() + "\n" + spb.getZeile4() + "\n" + spb.getZeile5()
        + "\n" + spb.getZeile6() + "\n" + spb.getZeile7() + "\n";
    rpt.addColumn(zuwendender, Element.ALIGN_LEFT);
    rpt.closeTable();

    switch (spb.getSpendenart())
    {
      case Spendenart.GELDSPENDE:
        rpt.addHeaderColumn("Betrag der Zuwendung -in Ziffern-",
            Element.ALIGN_CENTER, 100, Color.LIGHT_GRAY);
        break;
      case Spendenart.SACHSPENDE:
        rpt.addHeaderColumn("Wert der Zuwendung -in Ziffern-",
            Element.ALIGN_CENTER, 100, Color.LIGHT_GRAY);
        break;
    }
    rpt.addHeaderColumn("-in Buchstaben-", Element.ALIGN_CENTER, 250,
        Color.LIGHT_GRAY);
    rpt.addHeaderColumn("Tag der Zuwendung", Element.ALIGN_CENTER, 50,
        Color.LIGHT_GRAY);
    rpt.createHeader();
    Double dWert = (Double) spb.getBetrag();
    String sWert = "*" + Einstellungen.DECIMALFORMAT.format(dWert) + "*";
    rpt.addColumn(sWert, Element.ALIGN_CENTER);
    try
    {
      String betraginworten = GermanNumber.toString(dWert.longValue());
      betraginworten = "*" + betraginworten + "*";
      rpt.addColumn(betraginworten, Element.ALIGN_CENTER);
    }
    catch (Exception e)
    {
      Logger.error("Fehler", e);
      throw new RemoteException(
          "Fehler bei der Aufbereitung des Betrages in Worten");
    }

    String spendedatum = new JVDateFormatTTMMJJJJ()
        .format(spb.getSpendedatum());

    if (isSammelbestaetigung)
    {
      spendedatum = "(s. Anlage)";
    }

    rpt.addColumn(spendedatum, Element.ALIGN_CENTER);
    rpt.closeTable();

    switch (spb.getSpendenart())
    {
      case Spendenart.SACHSPENDE:
        rpt.addHeaderColumn(
            "Genaue Bezeichnung der Sachzuwendung mit Alter, Zustand, Kaufpreis usw.",
            Element.ALIGN_CENTER, 100, Color.LIGHT_GRAY);
        rpt.createHeader();
        rpt.addColumn(spb.getBezeichnungSachzuwendung(), Element.ALIGN_LEFT);
        rpt.closeTable();
        switch (spb.getHerkunftSpende())
        {
          case HerkunftSpende.BETRIEBSVERMOEGEN:
            rpt.add(
                "Die Sachzuwendung stammt nach den Angaben des Zuwendenden aus dem Betriebsverm�gen und ist "
                    + "mit dem Entnahmewert (ggf. mit dem niedrigeren gemeinen Wert) bewertet.\n\n",
                9);
            break;
          case HerkunftSpende.PRIVATVERMOEGEN:
            rpt.add(
                "Die Sachzuwendung stammt nach den Angaben des Zuwendenden aus dem Privatverm�gen.\n\n",
                9);
            break;
          case HerkunftSpende.KEINEANGABEN:
            rpt.add(
                "Der Zuwendende hat trotz Aufforderung keine Angaben zur Herkunft der Sachzuwendung gemacht.\n\n",
                9);
            break;
        }
        if (spb.getUnterlagenWertermittlung())
        {
          rpt.add(
              "Geeignete Unterlagen, die zur Wertermittlung gedient haben, z. B. Rechnung, Gutachten, liegen vor.\n\n",
              9);
        }
    }

    /*
     * Bei Sammelbest�tigungen ist der Verweis auf Verzicht in der Anlage
     * vermerkt
     */
    String verzicht = "nein";
    boolean andruckVerzicht = false;

    if (spb.getAutocreate())
    {
      if (!isSammelbestaetigung)
      {
        // if (buchungen.get(0).getVerzicht().booleanValue())
        if (spb.getBuchungen().get(0).getVerzicht().booleanValue())
        {
          verzicht = "ja";
        }
        andruckVerzicht = true;
      }
    }
    else
    {
      if (spb.getErsatzAufwendungen())
      {
        verzicht = "ja";
      }
      andruckVerzicht = true;
    }

    if (andruckVerzicht)
    {
      rpt.add("Es handelt sich um den Verzicht von Aufwendungen: " + verzicht
          + "\n\n", 9);
    }

    if (!Einstellungen.getEinstellung().getVorlaeufig())
    {
      String txt = "Wir sind wegen F�rderung "
          + Einstellungen.getEinstellung().getBeguenstigterzweck()
          + " nach dem letzten uns zugegangenen Freistellungsbescheid bzw. nach der Anlage zum K�rperschaftssteuerbescheid des Finanzamtes "
          + Einstellungen.getEinstellung().getFinanzamt()
          + ", StNr. "
          + Einstellungen.getEinstellung().getSteuernummer()
          + ", vom "
          + new JVDateFormatTTMMJJJJ().format(Einstellungen.getEinstellung()
              .getBescheiddatum())
          + " nach � 5 Abs. 1 Nr. 9 des K�rperschaftsteuergesetzes von der K�rperschaftsteuer und nach � 3 Nr. 6 des Gewerbesteuergesetzes von der Gewerbesteuer befreit.";
      rpt.add(txt, 9);
    }
    else
    {
      // rdc: "F�rderung" entfernt, da in "Beguenstigterzweck" enthalten
      String txt = "Wir sind wegen "
          + Einstellungen.getEinstellung().getBeguenstigterzweck()
          + " durch vorl�ufige Bescheinigung des Finanzamtes "
          + Einstellungen.getEinstellung().getFinanzamt()
          + ", StNr. "
          + Einstellungen.getEinstellung().getSteuernummer()
          + ", vom "
          + new JVDateFormatTTMMJJJJ().format(Einstellungen.getEinstellung()
              .getBescheiddatum())
          + " ab "
          + new JVDateFormatTTMMJJJJ().format(Einstellungen.getEinstellung()
              .getVorlaeufigab())
          + " als steuerbeg�nstigten Zwecken dienend anerkannt.";
      rpt.add(txt, 9);
    }
    // rdc: "F�rderung" entfernt, da in "Beguenstigterzweck" enthalten
    rpt.add("\n\nEs wird best�tigt, dass die Zuwendung nur zur "
        + Einstellungen.getEinstellung().getBeguenstigterzweck()
        + " verwendet wird.\n", 9);
    if (!Einstellungen.getEinstellung().getMitgliedsbetraege()
        && spb.getSpendenart() == Spendenart.GELDSPENDE)
    {
      rpt.add(
          "Es wird best�tigt, dass es sich nicht um einen Mitgliedsbeitrag i.S.v � 10b Abs. 1 Satz 2 Einkommensteuergesetzes handelt.",
          9);
    }

    if (isSammelbestaetigung)
    {
      rpt.add(new Paragraph(" "));
      rpt.add(
          "Es wird best�tigt, dass �ber die in der Gesamtsumme enthaltenen Zuwendungen keine weiteren Best�tigungen, weder formelle Zuwendungsbest�tigungen noch Beitragsquittungen oder �hnliches ausgestellt wurden und werden.",
          9);
    }

    rpt.add("\n\n" + Einstellungen.getEinstellung().getOrt() + ", "
        + new JVDateFormatTTMMJJJJ().format(spb.getBescheinigungsdatum()), 9);

    rpt.add(
        "\n\n\n\n.................................................................................\nUnterschrift des Zuwendungsempf�ngers",
        9);

    rpt.add("\n\nHinweis:", 9);
    rpt.add(
        "\nWer vors�tzlich oder grob fahrl�ssig eine unrichtige Zuwendungsbest�tigung erstellt oder wer veranlasst, dass "
            + "Zuwendungen nicht zu den in der Zuwendungsbest�tigung angegebenen steuerbeg�nstigten Zwecken verwendet "
            + "werden, haftet f�r die Steuer, die dem Fiskus durch einen etwaigen Abzug der Zuwendungen beim Zuwendenden "
            + "entgeht (� 10b Abs. 4 EStG, � 9 Abs. 3 KStG, � 9 Nr. 5 GewStG).\n\n"
            + "Diese Best�tigung wird nicht als Nachweis f�r die steuerliche Ber�cksichtigung der Zuwendung anerkannt, wenn das "
            + "Datum des Freistellungsbescheides l�nger als 5 Jahre bzw. das Datum der vorl�ufigen Bescheinigung l�nger als 3 Jahre "
            + "seit Ausstellung der Best�tigung zur�ckliegt (BMF vom 15.12.1994 - BStBl I S. 884).",
        8);

    /* Es sind mehrere Spenden f�r diese Spendenbescheinigung vorhanden */
    if (isSammelbestaetigung)
    {
      List<Buchung> buchungen = spb.getBuchungen();

      rpt.newPage();
      rpt.add(getAussteller(), 16);
      String datumBestaetigung = new JVDateFormatTTMMJJJJ().format(spb
          .getBescheinigungsdatum());
      rpt.add("Anlage zur Sammelbest�tigung vom " + datumBestaetigung, 13);

      String datumVon = new JVDateFormatTTMMJJJJ().format(buchungen.get(0)
          .getDatum());
      String datumBis = new JVDateFormatTTMMJJJJ().format(buchungen.get(
          buchungen.size() - 1).getDatum());
      rpt.add("f�r den Zeitraum vom " + datumVon + " bis " + datumBis, 13);

      rpt.add(new Paragraph(""));
      rpt.add(new Paragraph(""));

      rpt.addHeaderColumn("Zuwendungsart", Element.ALIGN_LEFT, 400,
          Color.LIGHT_GRAY);
      rpt.addHeaderColumn("Betrag", Element.ALIGN_LEFT, 100, Color.LIGHT_GRAY);
      rpt.addHeaderColumn("Datum", Element.ALIGN_LEFT, 100, Color.LIGHT_GRAY);
      rpt.addHeaderColumn("Verzicht auf Erstattung von Aufwendungen",
          Element.ALIGN_LEFT, 100, Color.LIGHT_GRAY);
      rpt.createHeader();

      boolean printBuchungsart = Einstellungen.getEinstellung()
          .getSpendenbescheinigungPrintBuchungsart();

      for (Buchung buchung : buchungen)
      {
        if (printBuchungsart)
        {
          rpt.addColumn(buchung.getBuchungsart().getBezeichnung(),
              Element.ALIGN_LEFT);
        }
        else
        {
          rpt.addColumn(buchung.getZweck(), Element.ALIGN_LEFT);
        }
        rpt.addColumn(Double.valueOf(buchung.getBetrag()));
        rpt.addColumn(buchung.getDatum(), Element.ALIGN_RIGHT);
        rpt.addColumn(buchung.getVerzicht().booleanValue());
      }

      /* Summenzeile */
      DecimalFormat f = new DecimalFormat("###,###.00");
      f = Einstellungen.DECIMALFORMAT;
      String sumString = f.format(spb.getBetrag());
      rpt.addColumn("Summe", Element.ALIGN_LEFT, Color.LIGHT_GRAY);
      rpt.addColumn(sumString, Element.ALIGN_RIGHT, Color.LIGHT_GRAY);
      rpt.addColumn("", Element.ALIGN_LEFT, Color.LIGHT_GRAY);
      rpt.addColumn("", Element.ALIGN_LEFT, Color.LIGHT_GRAY);

      rpt.closeTable();
    }

    rpt.close();
    fos.close();
  }

  private String getAussteller() throws RemoteException
  {
    return Einstellungen.getEinstellung().getNameLang() + ", "
        + Einstellungen.getEinstellung().getStrasse() + ", "
        + Einstellungen.getEinstellung().getPlz() + " "
        + Einstellungen.getEinstellung().getOrt();
  }
}