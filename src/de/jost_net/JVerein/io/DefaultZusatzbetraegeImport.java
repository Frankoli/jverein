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
package de.jost_net.JVerein.io;

import java.io.File;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import de.jost_net.JVerein.Einstellungen;
import de.jost_net.JVerein.io.Adressbuch.Adressaufbereitung;
import de.jost_net.JVerein.rmi.Mitglied;
import de.jost_net.JVerein.rmi.Zusatzbetrag;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.ProgressMonitor;

public class DefaultZusatzbetraegeImport implements Importer
{

  @Override
  public void doImport(Object context, IOFormat format, File file,
      String encoding, ProgressMonitor monitor) throws ApplicationException
  {
    monitor.setStatus(ProgressMonitor.STATUS_RUNNING);
    ResultSet results = null;
    Statement stmt = null;
    Connection conn = null;
    try
    {
      boolean fehlerInUeberschrift = false;

      Properties props = new java.util.Properties();
      props.put("separator", ";"); // separator is a bar
      props.put("suppressHeaders", "false"); // first line contains data
      props.put("charset", encoding);
      String path = file.getParent();
      String fil = file.getName();
      int pos = fil.lastIndexOf('.');
      props.put("fileExtension", fil.substring(pos));

      // load the driver into memory
      Class.forName("org.relique.jdbc.csv.CsvDriver");

      // create a connection. The first command line parameter is assumed to
      // be the directory in which the .csv files are held
      conn = DriverManager.getConnection("jdbc:relique:csv:" + path, props);

      // create a Statement object to execute the query with
      stmt = conn.createStatement();

      results = stmt.executeQuery("SELECT * FROM " + fil.substring(0, pos));
      String columnMitgliedsnummer = "Mitglieds_Nr";
      String colNachname = "Nachname";
      String colVorname = "Vorname";
      boolean b_mitgliedsnummer = false;
      boolean b_nachname = false;
      boolean b_vorname = false;
      ResultSetMetaData meta = results.getMetaData();
      for (int i = 1; i < meta.getColumnCount(); i++)
      {
        String columnTitle = meta.getColumnName(i);
        // Breche ab, wenn UTF-8 BOM vorhanden->
        // http://de.wikipedia.org/wiki/UTF-8#Byte_Order_Mark
        if (i == 1 && encoding.equals("UTF-8") && columnTitle.length() > 1)
        {
          if (Character.getNumericValue(meta.getColumnName(1).charAt(0)) == -1)
          {
            monitor.setStatus(ProgressMonitor.STATUS_ERROR);
            monitor
                .setStatusText("Eingelesene Datei beginnt mit UTF-8 BOM. Bitte entfernen. Abbruch!");
            fehlerInUeberschrift = true;
          }

        }
        if (columnTitle.equals(columnMitgliedsnummer))
        {
          b_mitgliedsnummer = true;
        }
        else if (columnTitle.equals(colNachname))
        {
          b_nachname = true;
        }
        else if (columnTitle.equals(colVorname))
        {
          b_vorname = true;
        }
      }
      if (b_mitgliedsnummer && (b_nachname || b_vorname))
      {
        monitor.setStatus(ProgressMonitor.STATUS_ERROR);
        monitor
            .setStatusText("Spalten�berschrift muss entweder nur Mitglieds_Nr oder Nachname und Vorname zur Zuordnung des Mitglieds enhalten. Es ist beides vorhanden. Abbruch!");
        fehlerInUeberschrift = true;
      }
      if (b_mitgliedsnummer == false
          && (b_nachname == false || b_vorname == false))
      {
        monitor.setStatus(ProgressMonitor.STATUS_ERROR);
        monitor
            .setStatusText("Spalten�berschrift muss entweder nur Mitglieds_Nr oder Nachname/Vorname zur Zuordnung des Mitglieds enhalten. Es ist weder noch vorhanden. Abbruch!");
        fehlerInUeberschrift = true;
      }
      if (fehlerInUeberschrift)
      {
        monitor.setStatus(ProgressMonitor.STATUS_ERROR);
        throw new ApplicationException("Fehler in Spalten�berschriften");
      }

      List<Zusatzbetrag> zusatzbetraegeList = new ArrayList<Zusatzbetrag>();
      if (fehlerInUeberschrift == false)
      {
        monitor
            .setStatusText("�berpr�fung der Spalten�berschriften erfolgreich abgeschlossen.");

        int anz = 0;
        boolean fehlerInDaten = false;
        while (results.next())
        {
          anz++;
          monitor.setPercentComplete(10);

          DBIterator list = Einstellungen.getDBService().createList(
              Mitglied.class);
          if (b_mitgliedsnummer)
          {
            list.addFilter("id = ? ", results.getString(columnMitgliedsnummer));
          }
          if (b_nachname)
          {
            list.addFilter("name = ? ", results.getString(colNachname));
          }
          if (b_vorname)
          {
            list.addFilter("vorname = ? ", results.getString(colVorname));
          }

          String mitgliedIdString = "";
          if (b_mitgliedsnummer)
            mitgliedIdString = columnMitgliedsnummer + "="
                + results.getString(columnMitgliedsnummer);
          else
            mitgliedIdString = colNachname + "="
                + results.getString(colNachname) + ", " + colVorname + "="
                + results.getString(colVorname);

          if (list.size() == 0)
          {
            monitor.setStatus(ProgressMonitor.STATUS_ERROR);
            monitor
                .setStatusText(MessageFormat
                    .format(
                        "F�r die Importzeile {0} ({1}) kein Mitglied in JVerein-Datenbank gefunden. Abbruch!",
                        anz + "", mitgliedIdString));
            fehlerInDaten = true;
          }
          else if (list.size() > 1)
          {
            monitor.setStatus(ProgressMonitor.STATUS_ERROR);
            monitor
                .setStatusText(MessageFormat
                    .format(
                        "F�r die Importzeile {0} ({1}) mehr als ein Mitglied gefunden. Abbruch!",
                        anz + "", mitgliedIdString));
            fehlerInDaten = true;
          }
          else
          // list.size() == 1
          {
            Mitglied m = (Mitglied) list.next();
            Zusatzbetrag zus = (Zusatzbetrag) Einstellungen.getDBService()
                .createObject(Zusatzbetrag.class, null);
            zus.setMitglied(new Integer(m.getID()));
            double betrag = results.getDouble("Betrag");
            if (betrag == 0)
            {
              monitor
                  .setStatusText(MessageFormat
                      .format(
                          "F�r die Importzeile {0} ({1}) konnte die Flie�kommazahl in der Spalte Betrag nicht verarbeitet werden. Zahl muss gr��er 0 sein. Abbruch!",
                          anz + "", mitgliedIdString));
              fehlerInDaten = true;
            }
            zus.setBetrag(betrag);
            String buchungstext = results.getString("Buchungstext");
            if (buchungstext.length() > 27)
            {
              monitor
                  .setStatusText(MessageFormat
                      .format(
                          "F�r die Importzeile {0} ({1}) konnte der Text in der Spalte Buchungstext nicht verarbeitet werden. L�nge des Buchungstextes ist auf 27 Buchstaben begrenzt. Abbruch!",
                          anz + "", mitgliedIdString));
              fehlerInDaten = true;
            }
            zus.setBuchungstext(buchungstext);
            try
            {
              buchungstext = results.getString("Buchungstext2");
              if (buchungstext.length() > 27)
              {
                monitor
                    .setStatusText(MessageFormat
                        .format(
                            "F�r die Importzeile {0} ({1}) konnte der Text in der Spalte Buchungstext nicht verarbeitet werden. L�nge des Buchungstextes ist auf 27 Buchstaben begrenzt. Abbruch!",
                            anz + "", mitgliedIdString));
                fehlerInDaten = true;
              }
              zus.setBuchungstext2(buchungstext);
            }
            catch (SQLException e)
            {
              Logger.error("Fehler", e);
            }
            try
            {
              Date d = de.jost_net.JVerein.util.Datum.toDate(results
                  .getString("F�lligkeit"));
              zus.setFaelligkeit(d);
              zus.setStartdatum(d);
            }
            catch (ParseException e)
            {
              monitor
                  .setStatusText(MessageFormat
                      .format(
                          "F�r die Importzeile {0} ({1}) konnte das Datum in der Spalte F�lligkeit nicht verarbeitet werden. Abbruch!",
                          anz + "", mitgliedIdString));
              fehlerInDaten = true;
            }

            int intervall = results.getInt("Intervall");
            if (intervall < 0)
            {
              monitor
                  .setStatusText(MessageFormat
                      .format(
                          "F�r die Importzeile {0} ({1}) konnte die Zahl in der Spalte Intervall nicht verarbeitet werden. Zahl muss gr��er oder gleich 0 sein. Abbruch!",
                          anz + "", mitgliedIdString));
              fehlerInDaten = true;
            }
            zus.setIntervall(intervall);
            try
            {
              String datum = results.getString("Endedatum");
              if (datum.length() > 0)
              {
                Date d = de.jost_net.JVerein.util.Datum.toDate(results
                    .getString("Endedatum"));
                zus.setEndedatum(d);
              }
            }
            catch (ParseException e)
            {
              monitor
                  .setStatusText(MessageFormat
                      .format(
                          "F�r die Importzeile {0} ({1}) konnte das Datum in der Spalte F�lligkeit nicht verarbeitet werden. Abbruch!",
                          anz + "", mitgliedIdString));
              fehlerInDaten = true;
            }

            zusatzbetraegeList.add(zus);
          }

        }
        // �berpr�fen und parsen der Daten beendet.
        if (fehlerInDaten == false)
        {
          monitor
              .setStatusText(MessageFormat
                  .format(
                      "�berpr�fen aller Zusatzbeitr�ge erfolgreich abschlossen. {0} Zusatzbeitr�ge werden importiert...",
                      anz + ""));
          int count = 0;
          for (Zusatzbetrag zusatzbetrag : zusatzbetraegeList)
          {
            anz++;
            monitor.setPercentComplete(10 + (count * 90 / zusatzbetraegeList
                .size()));
            zusatzbetrag.store();
            monitor.setStatusText(MessageFormat.format(
                "Zusatzbeitrag f�r Mitglied {0} erfolgreich importiert. ",
                Adressaufbereitung.getNameVorname(zusatzbetrag.getMitglied())));
          }
          monitor.setStatusText("Import komplett abgeschlossen.");
        }
        else
        {
          // if(fehlerInDaten)
          monitor.setStatus(ProgressMonitor.STATUS_ERROR);
          throw new ApplicationException("Fehler in Daten. Import abgebrochen.");
        }
      }

    }
    catch (RemoteException e)
    {
      Logger.error("Fehler", e);
      throw new ApplicationException("Fehler beim Importieren: "
          + e.getMessage());
    }
    catch (SQLException e)
    {
      Logger.error("Fehler", e);
      throw new ApplicationException("Fehler beim Importieren:"
          + e.getMessage());
    }
    catch (ClassNotFoundException e)
    {
      Logger.error("Fehler", e);
      throw new ApplicationException(
          "Treiber zum Parsen der CSV-Datei nicht gefunden.");
    }
    finally
    {
      try
      {
        results.close();
      }
      catch (SQLException e)
      {
        Logger.error("Fehler beim Schlie�en von ResultSet results", e);
        e.printStackTrace();
      }
      try
      {
        stmt.close();
      }
      catch (SQLException e)
      {
        Logger.error("Fehler beim Schlie�en von Statement stmt", e);
        e.printStackTrace();
      }
      try
      {
        conn.close();
      }
      catch (SQLException e)
      {
        Logger.error("Fehler beim Schlie�en von Connection conn", e);
        e.printStackTrace();
      }
    }
  }

  @Override
  public String getName()
  {
    return "Default-Zusatzbetr�ge";
  }

  public boolean hasFileDialog()
  {
    return true;
  }

  @Override
  public IOFormat[] getIOFormats(Class<?> objectType)
  {
    if (objectType != Zusatzbetrag.class)
    {
      return null;
    }
    IOFormat f = new IOFormat()
    {

      @Override
      public String getName()
      {
        return DefaultZusatzbetraegeImport.this.getName();
      }

      /**
       * @see de.willuhn.jameica.hbci.io.IOFormat#getFileExtensions()
       */
      @Override
      public String[] getFileExtensions()
      {
        return new String[] { "*.csv" };
      }
    };
    return new IOFormat[] { f };
  }
}
