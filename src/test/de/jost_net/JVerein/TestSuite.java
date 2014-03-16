/**********************************************************************
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
package test.de.jost_net.JVerein;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import test.de.jost_net.JVerein.io.AltersgruppenParserTest;
import test.de.jost_net.JVerein.io.Adressbuch.AdressaufbereitungTest;
import test.de.jost_net.JVerein.util.TableColumnReplacerTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({ AdressaufbereitungTest.class,
    AltersgruppenParserTest.class, TableColumnReplacerTest.class })
public class TestSuite
{
  // Nothing to do
}
