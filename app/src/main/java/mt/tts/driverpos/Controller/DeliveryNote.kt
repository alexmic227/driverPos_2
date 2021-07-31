package mt.tts.driverpos.Controller

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.text.format.DateFormat
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.android.synthetic.main.activity_delivery_note.*
import mt.tts.driverpos.R
import mt.tts.driverpos.Utilities.DataStore
import java.io.*
import java.util.*


class DeliveryNote : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_note)

        var ccode = intent.getStringExtra("ClientCode")
        var qty_s = intent.getStringExtra("QTY")

        var customer = DataStore.GetCustomer(ccode.toString())
        scr02_DelvDistNme.text = customer.ClientCode
        scr02_DelvNoteAddrTxt.text = customer.Address
        scr02_DelvDistQty.text = qty_s

        val settings: SharedPreferences = applicationContext.getSharedPreferences(
                "driverpos",
                MODE_PRIVATE
        )
        val driverID: String = settings.getString("driverid", "X").toString()

        var dt = Date()

        var qty = qty_s.toString().toDouble()
        var price = customer.Price.toDouble()
        var netprice = price.times(qty.toDouble())
        var vatRate = 18
        var vatAmount = netprice * vatRate / 100
        var totalPrice = netprice + vatAmount

        var orderNumber = driverID + DateFormat.format("yyyyMMdd", dt).toString()
        var orderDate = DateFormat.format("yyyy-MM-dd", dt).toString()
        var orderTime = DateFormat.format("hh:mm:ss a", dt).toString()

         var slip   = "Order Number : " + orderNumber + "\n" +
        "Date " + orderDate + "\n" +
        "Time " + orderTime + "\n" +
        "To: " + customer.ClientCode + " " + customer.Address + "\n" +
        "VAT Reg No:" + customer.VAT + "\n" +
        "------------------------------------------------\n" +
        "Litres " + qty_s + "\n" +
        "Unit Price " + customer.Price + "\n" +
        "Net Price " + netprice + "\n" +
        "VAT Rate " + vatRate.toString() + "\n" +
        "Total VAT: " + vatAmount + "\n" +
        "Total: " + totalPrice + "\n" +
        "================================================\n" +
        "Previous Balance " + customer.PreviousBalance + "\n" +
        "Payment Type: " + customer.CashInvoice + "\n"

        //Delivery note is in HTML
        var header = "<head> </head>"
        var footer = ""

        var signaturebmp = intent.getStringExtra("BMP")
        var signatureimg = "<img width=\"200\" src=\"data:image/png;base64, " + signaturebmp + "\" />"

        val html = "<!DOCTYPE html>\r\n<html lang=\"en\">" + header + "<body> <h2>DriverPos</h2> <h3>Delivery Note</h3> <pre>" + slip + "</pre> <hr />Signature:<br /> " + signatureimg + " <br />Thank you for your custom." + footer + " </body></html>"
        val webSettings: WebSettings = printSlip.getSettings()
        webSettings.javaScriptEnabled = true
        printSlip.setWebViewClient(WebViewClient())
        printSlip.setWebChromeClient(WebChromeClient())
        printSlip.loadData(html, "text/html", "UTF8")

        val dir = File(applicationContext.getFilesDir(), "DriverPOSData")
        if (!dir.exists()) {
            dir.mkdir()
        }

        DeliveryNoteHTML = html

        //We make a copy of the delivery Note (html file)
        try {
            val ffile = File(
                    dir,
                    "O_" + orderNumber + "_" + DateFormat.format("hhmmss", dt).toString() + ".html"
            )
            val writer = FileWriter(ffile)
            writer.append(html)
            writer.flush()
            writer.close()

            DeliveryNoteShareFile = ffile
        } catch (e: Exception) {
        }

        scr_02_Main.setOnClickListener {
            val mainActivityIntent = Intent(this, MainActivity::class.java)
            startActivity(mainActivityIntent)
        }

        scr_02_Print_Btn.setOnClickListener {

            ShareEmail = settings.getString("shareemail", "").toString()
            TStamp = orderNumber
            CustomerEmail = customer.Email
            DriverID = driverID
            DT = dt

            var success = createWebPrintJob(printSlip, orderNumber)
            if (success)
            {
                //We save the order to output CSV file
                if (!customer.Locked) {
                    try {
                        val outputFilename: String = applicationContext.getFilesDir().toString() + "/DriverPOSData/" + "output.csv"
                        val outputFile = FileWriter(outputFilename, true)
                        outputFile.write(customer.ClientCode + "," + orderDate + "," + orderTime + "," + orderNumber + "," + qty_s + "," + totalPrice + "\r\n");
                        outputFile.flush();
                        outputFile.close();
                    } catch (e: Exception) {
                    }
                }

                customer.Locked = true //entry is now locked
            }
        }
    }

    fun createWebPrintJob(webView: WebView, orderNum: String) : Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val printManager = this.getSystemService(Context.PRINT_SERVICE) as PrintManager

            val printAdapter: PrintDocumentAdapter = CustomPDAdapterWrapper(webView.createPrintDocumentAdapter("DeliveryNote" + orderNum), ::shareFile)

            printManager.print(
                    "DeliveryNote" + orderNum,
                    printAdapter,
                    PrintAttributes.Builder().build()
            )

            return true
        } else {
            Toast.makeText(getApplicationContext(), "Failed to Print", Toast.LENGTH_LONG).show();
            return false
        }
    }

    private lateinit var DeliveryNoteShareFile: File
    private lateinit var DeliveryNoteHTML: String
    private lateinit var ShareEmail: String
    private lateinit var CustomerEmail: String
    private lateinit var TStamp: String
    private lateinit var DriverID: String
    private lateinit var DT: Date

    private val WRITE_REQUEST_CODE = 101

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WRITE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK ->
                    if (data != null && data.data != null) {
                        writeInFile(data.data!!, DeliveryNoteHTML)
                }
                RESULT_CANCELED -> {
                }
            }
        }
    }

    private fun writeInFile(uri: Uri, text: String) {
        val outputStream: OutputStream?
        try {
            outputStream = contentResolver.openOutputStream(uri)
            val bw = BufferedWriter(OutputStreamWriter(outputStream))
            bw.write(text)
            bw.flush()
            bw.close()
        } catch (e: IOException) {
        }
    }

    fun saveFile()
    {
        //This function is called after Sending Email is Done
        //We Save the Delivery Note to storage

        val intentShareFile = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intentShareFile.addCategory(Intent.CATEGORY_OPENABLE)
        intentShareFile.type = "text/html"
        intentShareFile.putExtra(Intent.EXTRA_TITLE, "DeliveryNote" + TStamp + ".html")
        startActivityForResult(intentShareFile, WRITE_REQUEST_CODE);
    }

    fun shareFile()
    {
        //This function is called after Printing is Finished

        //=============================================================================
        saveFile() //if saving file on device storage is not required comment this line
        //=============================================================================

        //We Share the Delivery Note via email
        val intentShareFile = Intent(Intent.ACTION_SEND)
        val exportUri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                DeliveryNoteShareFile)

        intentShareFile.type = "text/html"
        intentShareFile.putExtra(Intent.EXTRA_STREAM, exportUri)
        intentShareFile.putExtra(Intent.EXTRA_EMAIL, arrayOf(ShareEmail, CustomerEmail))
        intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Delivery Note (" + TStamp + ")...")
        intentShareFile.putExtra(Intent.EXTRA_TEXT, "Delivery Note Export from driver " + DriverID + " on " + DateFormat.format("yyyy-MM-dd hh:mm:ss", DT).toString() + " for order " + TStamp + ".")
        startActivity(intentShareFile)
    }

}

class CustomPDAdapterWrapper(private val delegate: PrintDocumentAdapter, private val sharefin: () -> (Unit)) : PrintDocumentAdapter() {
    override fun onFinish() {
        delegate.onFinish()
        sharefin(); //share the file on finish printing
    }

    override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback?, extras: Bundle?) {
        delegate.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)
    }

    override fun onStart() {
        delegate.onStart()
    }

    override fun onWrite(pages: Array<out PageRange>?, destination: ParcelFileDescriptor?, cancellationSignal: CancellationSignal?, callback: WriteResultCallback?) {
        delegate.onWrite(pages, destination, cancellationSignal, callback)
    }

}