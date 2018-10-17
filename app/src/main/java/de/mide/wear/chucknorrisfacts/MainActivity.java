package de.mide.wear.chucknorrisfacts;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Activity fetches "Chuck Norris Facts" (short jokes) from Web-API and displays them.
 * <br><br>
 *
 * Alternate approach for refresh of joke might be
 * {@link android.support.v4.widget.SwipeRefreshLayout}, see
 * <a href="http://www.vogella.com/tutorials/SwipeToRefresh/article.html">this tutorial</a>.
 * <br><br>
 *
 * This project is licensed under the terms of the BSD 3-Clause License.
 * <br><br>
 *
 * The author of this app is neither related to Chuck Norris nor to the
 * developer/provider of the Web-API <a href="http://www.icndb.com/api/">icndb.com</a>.
 */
public class MainActivity extends WearableActivity
                          implements View.OnClickListener {

    /** Kennzeichnungs-String ("Schildchen") für alle Log-Nachrichten in dieser App. */
    public static final String TAG4LOGGING = "ChuckNorris";


    /** UI-Element zur Darstellung von Ergebnis und Fehlermeldungen. */
    protected TextView _textView = null;

    /** Flag, um mehrere gleichzeitige Ladevorgänge zu verhindern. */
    protected boolean _ladevorgangLaueft = false;


    /**
     * Lifecycle-Methode. Nachdem alle Initialisierungen vorgenommen sind
     * wird gleich ein Witz geladen.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _textView = (TextView) findViewById(R.id.text);
        _textView.setOnClickListener(this);

        setAmbientEnabled(); // Enables Always-on

        ladeWitzImHintergrund();
    }


    /**
     * Event-Handler-Methode, lädt neuen Witz (wenn nicht bereits ein anderer
     * Ladevorgang läuft).
     *
     * @param view  Element, das Event ausgelöst hat.
     */
    @Override
    public void onClick(View view) {
        if (_ladevorgangLaueft == true) {
            Log.i(TAG4LOGGING, "Es läuft schon ein Ladevorgang.");
            return;
        }
        ladeWitzImHintergrund();
    }
    
    
    /**
     * Erzeugt Thread-Objekt und startet es auch gleich.
     */
    protected void ladeWitzImHintergrund() {
    
        MeinThread mt = new MeinThread();
        mt.start(); 
        
        // Kurzform: new MeinThread().start();
    }


    /**
     * Methode übergibt Runnable-Objekt an Main-Thread, um
     * als Argument übergebenen Text in TextView zu setzen
     * (ändernde UI-Zugriffe sollten nur aus Main-Thread
     *  heraus vorgenommen werden).
     *
     * @param text  Text, der in TextView angezeigt werden soll.
     */
    protected void zeigeTextInMainThread(String text) {

        // Für Instanz von anonymer Klasse muss der String (Closure)
        // muss die lokale Variable nicht-änderbar sein.
        final String text2 = text;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                _textView.setText(text2);
            }
        };

        runOnUiThread(runnable);

        // Alternative zu runOnUIThread(): Runnable-Objekt an Methode "post()"
        // von beliebigem View-Element übergeben.
        //_textView.post(runnable);
    }


    /* *************************************** */
    /* ********* Start innere Klasse ********* */
    /* *************************************** */

    /**
     * Innere Klasse für Netzwerk-Zugriff und Auswertung Ergebnis-Dokument
     * (Parsen der JSON-Datei, die als Antwort von der Web-API geschickt wurde).
     * Nach Erzeugung einer Instanz dieser Klasse muss die Methode
     * {@link Thread#start()} aufgerufen werden (<b>NICHT</b> die
     * Methode {@link Thread#run()} direkt aufrufen, sonst wird der
     * Code nicht in einem Hintergrund-Thread ausgeführt).
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

            zeigeTextInMainThread( getString(R.string.loading) );

            String ergebnisStr = holeWitz();
            if (ergebnisStr.length() > 0) {

                try {
                    String witz = extrahiereWitzAusJson( ergebnisStr );
                    zeigeTextInMainThread( witz );
                }
                catch (JSONException ex) {
                    Log.e(TAG4LOGGING, "Fehler beim Parsen des JSON-Strings: " + ex.getMessage());
                    zeigeTextInMainThread( getString(R.string.error) + ex.getMessage() );
                }

            } else {
                Log.w(TAG4LOGGING, "Leerer String als JSON-Antwort von Methode holeWitz() erhalten.");
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
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET"); // Eigentlich nicht nötig, weil "GET" Default-Wert ist.

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {

                zeigeTextInMainThread( getString(R.string.http_error) + conn.getResponseMessage() );

            } else {

                InputStream is        = conn.getInputStream();
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
            zeigeTextInMainThread( getString(R.string.error) + ex.getMessage() );
        }
        finally {
            if (conn != null) { conn.disconnect(); }
            _ladevorgangLaueft = false;
        }

        return httpErgebnisDokument;
    }


    /**
     * Methode für parsen der JSON-Antwort von der Web-API, um den eigentlichen
     * Witz zu erhalten.
     *
     * @param jsonString  Komplettes JSON-Dokument, das von der Web-API geliefert wurde
     *
     * @return  String mit Kurzwitz
     *
     * @throws JSONException  Fehler beim Parsen des JSON-Objekts; wird geworfen, wenn
     *                        für einen bestimmten Key kein Wert des entsprechenden
     *                        Datentypes gefunden wird.
     */
    protected String extrahiereWitzAusJson(String jsonString) throws JSONException {

        JSONObject mainObjekt = new JSONObject(jsonString);

        String typeString = mainObjekt.getString("type");

        if (typeString.equalsIgnoreCase("success") == false) {
            throw new JSONException( getString( R.string.status_not_success) + typeString );
        }

        // Unterobjekt mit dem eigentlichen Witz holen.
        JSONObject unterObjekt = mainObjekt.getJSONObject("value");

        int witzID        = unterObjekt.getInt("id");
        String witzString = unterObjekt.getString("joke");
        // unterObjekt hat unter dem Key "categories" noch einen Array mit den
        // gewählten Kategorien (z.B. "nerdy" oder "explicit"), aber das
        // lesen wir nicht aus.

        Log.i(TAG4LOGGING, "Witz " + witzID + ": " + witzString);

        // Sonderzeichen ersetzen
        witzString = witzString.replace("&quot;", "\""); // ggf. Anführungszeichen ersetzen

        return witzString;
    }

}
