package mt.tts.driverpos.Utilities

import android.util.Log
import com.opencsv.CSVReader
import mt.tts.driverpos.Model.Customer
import java.io.BufferedReader
import java.lang.NullPointerException

class ReadCSV {
    companion object {
        fun ReadClientsCSV(bufferedReader: BufferedReader, driverID : String) {
            val reader = CSVReader(bufferedReader)
            try {
                var nextLine: Array<String>
                while (reader.readNext().also { nextLine = it } != null) {
                    // nextLine[] is an array of values from the line
                    if (nextLine[1].equals("driver", true))  {
                        if (!(nextLine[2].equals(driverID, true)))
                        {
                            throw java.lang.Exception("Incorrect File - Driver ID Mismatch")
                        }
                    }
                    else if (!((nextLine[0].equals("address", true)) || (nextLine[1].equals("Postcode", true)))) {
                        val customr = Customer(nextLine[0],
                                nextLine[1],
                                nextLine[2],
                                nextLine[3],
                                nextLine[4].toDouble(),
                                nextLine[5],
                                nextLine[6].toDouble(),
                                nextLine[7].toDouble(),
                                nextLine[8],
                                nextLine[9])

                        DataStore.instance.customers.add(customr)
                    }
                }
            }
            catch (npe: NullPointerException) {
            }
            catch (e: Exception) {
                Log.d("dposr", e.toString())
                throw e
            }
            bufferedReader.close()
        }

        /*
        outputFile.write(customer.ClientCode + "," + orderDate + "," + orderTime + "," + orderNumber + "," + qty_s + "," + totalPrice + "\r\n");
         */

        fun ReadOutputCSV(bufferedReader: BufferedReader) {
            val reader = CSVReader(bufferedReader)
            try {
                var nextLine: Array<String>
                while (reader.readNext().also { nextLine = it } != null) {
                    // nextLine[] is an array of values from the line
                    if (!((nextLine[0].equals("ClientCode", true)) || (nextLine[1].equals("OrderDate", true)))) {
                        val clientCode = nextLine[0];
                        val qty_s = nextLine[4];

                        try {
                            var customer = DataStore.GetCustomer(clientCode)
                            customer.Qty = qty_s.toDouble()
                            customer.Locked = true
                        }
                        catch (e: Exception) {
                            Log.d("dposr", e.toString())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("dposr", e.toString())
            }
            bufferedReader.close()
        }
    }
}