package de.mide.wear.chucknorrisfacts;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends WearableActivity
                          implements View.OnClickListener {

    public static final String TAG4LOGGING = "ChuckNorris";


    /** UI-Element zur Darstellung von Ergebnis und Fehlermeldungen. */
    protected TextView _textView = null;

    /** Flag, um mehrere gleichzeitige Ladevorgänge zu verhindern. */
    protected boolean _ladevorgangLaueft = false;


    /**
     * Lifecycle-Methode. Nachdem alle Initialisierungen vorgenommen sind
     * wird ein Witz geladen.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _textView = (TextView) findViewById(R.id.text);
        _textView.setOnClickListener(this);

        // Enables Always-on
        setAmbientEnabled();

        MeinThread mt = new MeinThread();
        mt.start();
    }


    /**
     * Event-Handler-Methode, lädt neuen Witz (wenn nicht bereits ein anderer
     * Ladevorgang läuft).
     *
     * @param view Element, das Event ausgelöst hat.
     */
    @Override
    public void onClick(View view) {
        if (_ladevorgangLaueft == true) {
            Log.i(TAG4LOGGING, "Es läuft schon ein Ladevorgang.");
            return;
        }

        MeinThread mt = new MeinThread();
        mt.start();
    }


    /**
     * Methode übergibt Runnable-Objekt an Main-Thread, um
     * als Argument übergebenen Text in TextView zu setzen
     * (ändernde UI-Zugriffe sollten nur aus Main-Thread
     *  heraus vorgenommen werden).
     *
     * @param text Text, der in TextView angezeigt werden soll.
     */
    protected void zeigeTextInMainThread(String text) {

        final String text2 = text;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                _textView.setText(text2);
            }
        };

        runOnUiThread(runnable);
    }


    /* *************************************** */
    /* ********* Start innere Klasse ********* */
    /* *************************************** */

    /**
     * Innere Klasse für Netzwerk-Zugriff und Auswertung
     * Ergebnis-Dokument,
     */
    protected class MeinThread extends Thread {

        /**
         * Inhalt der run()-Methode einer Unterklasse von Thread
         * wird in einem Hintergrund-Thread ausgeführt, wenn
         * das Thread-Objekt mit der Methode {@link Thread#start()}
         */
        @Override
        public void run() {

            _ladevorgangLaueft = true;

            zeigeTextInMainThread("Lade ...");

            String ergebnisStr = holeWitz();
            if (ergebnisStr.length() > 0) {
                zeigeTextInMainThread(ergebnisStr);
            }
        }

    };

    /* *************************************** */
    /* ********* Ende innere Klasse  ********* */
    /* *************************************** */


    /**
     * Methode mit HTTP-Zugriff, muss in Hintergrund-Thread ausgeführt werden!
     *
     * @return HTTP-Response-String oder leerer String bei Fehler (aber nicht <i>null</i>).
     */
    protected String holeWitz() {

        HttpURLConnection conn      = null;
        String httpErgebnisDokument = "";

        try {
            URL url = new URL("http://api.icndb.com/jokes/random?limitTo=[nerdy]");
            conn = (HttpURLConnection)url.openConnection();
            //conn.setRequestMethod("GET"); // Default

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {

                zeigeTextInMainThread("HTTP-Fehler: " + conn.getResponseMessage());

            } else {

                InputStream is = conn.getInputStream();
                InputStreamReader ris = new InputStreamReader(is);
                BufferedReader reader = new BufferedReader(ris);

                String zeile = "";
                while ( (zeile = reader.readLine()) != null) {
                    httpErgebnisDokument += zeile;
                }
            }
        }
        catch (Exception ex) {
            Log.e(TAG4LOGGING, "Fehler beim HTTP-Zugriff: " + ex.getMessage());
            zeigeTextInMainThread("Fehler: " + ex.getMessage());
        }
        finally {
            if (conn != null) { conn.disconnect(); }
            _ladevorgangLaueft = false;
        }

        return httpErgebnisDokument;
    }

}
