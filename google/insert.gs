/*
 * Copyright 2016 Sam Crow
 *
 * This file is part of JRBP Survey.
 *
 * JRBP Survey is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JRBP Survey is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRBP Survey.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * This Google script accepts key/value pairs from an HTTP POST request and inserts them into the first sheet of a spreadsheet.
 *
 * The script looks for headers in row 1 of the spreadsheet. For each HTTP POST parameter that matches a header,
 * it inserts the corresponding value into the spreadsheet. It inserts only one row each time it is invoked.
 *
 * When it completes, it returns JSON with a "result" key corresponding to either "success" or "error", and a "message" key
 * containing additional information.
 */

/*
 * Instructions for setting up this script:
 *
 * 1. Create a spreadsheet with headers in row 1
 * 2. From the spreadsheet editor, open the Tools menu and select 'Script Editor'. A new script project will open.
 * 3. Give the project a name
 * 4. Paste this script into the Code.gs file
 * 5. Set the sheetID variable to the ID of your spreadsheet. The sheet ID is a long series of letters and numbers that can be found in the URL of your spreadsheet.
 * 6. Run the script. This will not modify the spreadsheet, but it will prompt you to grant the project access to your spreadsheets.
 *    Grant the access.
 * 7. Open the Publish menu and select 'Deploy as web app'. Set 'Execute the app as' to 'Me' and 'Who as access to the app' to 'Anyone (even anonymous)'.
 * 8. Copy the web app URL. You can send POST requests to that URL to run the script.
 */

function doPost(e) {
  if (!e) return;

  // The ID of the spreadsheet to insert into
  var sheetID = "1TMBLRNYbcvIP5KG83BZ4NprRxCsfX6tXZJTD42ZnnHM";
  var status = {};

  var lock = LockService.getScriptLock();
  lock.waitLock(30000);

  try {
    var sheet = SpreadsheetApp.openById(sheetID).getSheets()[0];
    // Get headers from row 1 of the sheet
    var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
    // Add the data
    var column = [];
    var row = [];
    // Maps from column names to values
    var input = {};

    // Copy each form parameter
    for (var keys in e.parameter) {
      input[keys] = e.parameter[keys];
    }

    // For each parameter that matches a heading, add the value to the row
    // If no parameter matches, add an empty string
    for (i in headers) {
      column = headers[i];
      row.push(input[column] || "");
    }

    if (row.length) {
      sheet.appendRow(row);
      status = {
        result: "success",
        message: "Row added at position " + sheet.getLastRow(),
        values: row
      };

    } else {
      status = {
        result: "error",
        message: "No recognized values were entered"
      };
    }

  } catch (e) {
    status = {
      result: "error",
      message: e.toString()
    };
  } finally {
    lock.releaseLock();
  }

  return ContentService
    .createTextOutput(JSON.stringify(status))
    .setMimeType(ContentService.MimeType.JSON);
}
