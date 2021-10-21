package org.izv.aff.consultaagendaad;

import static android.content.ContentValues.TAG;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.UserDictionary;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.izv.aff.consultaagendaad.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    private final int CONTACT_PERMISSION = 1;

    private Button bt_search; // = findViewById(R.id.bt_search);
    private EditText etPhone;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //si hace return true es que este metodo se encarga de hacer algo si no lo hara otra clase u otro metodo.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //https://www.develou.com/como-crear-actividad-preferencias-android/#Crear_Una_Actividad_De_Preferencias_En_Android
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void initialize() {
        bt_search = findViewById(R.id.bt_search);
        etPhone = findViewById(R.id.etPhone);
        tvResult = findViewById(R.id.tvResult);

        //Inicializamos los ultimos datos q hemos dejado que estaran guardados en preferences
        SharedPreferences preferenciasActividad = getPreferences(Context.MODE_PRIVATE);
        String lastSearch = preferenciasActividad.getString(getString(R.string.last_search), "");//Si no existe devuelve cadena vacia
        if (!lastSearch.isEmpty()) {
            etPhone.setText(lastSearch);
        }

        bt_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchIfPermitted();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void explain() {
        showRationaleDialog(getString(R.string.title),
                                      getString(R.string.message),
                                      Manifest.permission.READ_CONTACTS,
                                      CONTACT_PERMISSION);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermission() {
        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, CONTACT_PERMISSION);
    }

    private void search() {
        String phone = etPhone.getText().toString();
        tvResult.setText("");
        //Contexto de la aplicacion y contexto de la actividad: Los dos sirven para indicar de alguna manera donde va a trabajar(que actividad o aplicacion pero q actividad q normalmente es la actual)
        //Acceso a contexto de la propia actividad, nos pone dentro de la actividad q esta en ese momento utilizandose.
        SharedPreferences preferenciasActividad = getPreferences(Context.MODE_PRIVATE);
        //Para escribir necesitamos un editor(4 editores)
        SharedPreferences.Editor editor = preferenciasActividad.edit();//MainActivity.xml
        //Guardarlos
        editor.putString(getString(R.string.last_search), phone);
        editor.commit();
        //Cuando necesite entrar en setting voy a necesitar utilizar si o si este PreferenceManager.getDefaultSharedPreferences(this)


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this); //Obtenemos la preferencias compartidas en el objeto.
        String email = sharedPreferences.getString(getString(R.string.settings_email), getString(R.string.no_email)); //Le pasamos la KEY y de segundo parametro ponemos lo que nos va a devolver si no existe. Leemos el email.
        //sharedPreferences tenemos que ponerle un string del string.xml. No coge R.string.no_email necesita el metodo getstring porq deveulve un int no la cadena cuando pongo R.string.no_email
        tvResult.append(email + "\n"); //En el text cada clic añadimos el correo de settings.
        //-----------------------------------

        phone = searchFormat(phone);
        Uri uri2 = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String proyeccion2[] = new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
        String seleccion2 = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " like ?";
        String argumentos2[] = new String[]{phone};
        String orden2 = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;
        Cursor cursor2 = getContentResolver().query(uri2, proyeccion2, seleccion2, argumentos2, orden2);
        String[] columnas2 = cursor2.getColumnNames();

        String nombre, numero;
        int columnaNombre = cursor2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int columnaNumero = cursor2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        while(cursor2.moveToNext()) {
            nombre = cursor2.getString(columnaNombre);
            numero = cursor2.getString(columnaNumero);
            tvResult.append(nombre + " " + numero + "\n ->");
            for (String s: columnas2) {
                int pos = cursor2.getColumnIndex(s);
                String valor = cursor2.getString(pos);
                tvResult.append(s + " " + valor + "\n");
            }
        }
    }

    private String searchFormat(String phone) {
        String newString = "";
        for (char ch: phone.toCharArray()){// Convierto la cadena pero con % entre cada caracter para que encuentre todos los contactos relacionados.
            newString += ch + "%";
        }
        return newString;
    }

    private void searchIfPermitted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // La version de android es posterior a la 6 incluida
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Ya tengo el permiso
                search();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                explain();
            } else {
                requestPermission();
            }
        } else {
            // La version de android es anterior a la 6
            // Ya tengo el permiso
            search();
        }
    }

    private void showRationaleDialog(String title, String message, String permission, int requestCode) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Si pulso negativo no quiero hacer nada
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Si pulso positivo quiero pedir los permisos
                        requestPermission();
                    }
                });


        builder.create().show();

    }

}

    //Contexto de la aplicacion y contexto de la actividad: Los dos sirven para indicar de alguna manera donde va a trabajar(que actividad o aplicacion pero q actividad q normalmente es la actual)
    //Acceso a contexto de la propia actividad, nos pone dentro de la actividad q esta en ese momento utilizandose.
   /* SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this); //Obtenemos la preferencias compartidas en el objeto.
    SharedPreferences p1 = getSharedPreferences("preferenciascompartidas", Context.MODE_PRIVATE);
    SharedPreferences p2 = getPreferences(Context.MODE_PRIVATE);
    SharedPreferences p3 = PreferenceManager.getDefaultSharedPreferences(this);//Preferencia compartida por defecto de la aplicacion.
    SharedPreferences p4 = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    //Para escribir necesitamos un editor(4 editores)
    SharedPreferences.Editor ed1 = p1.edit();//preferenciascompartidas
    SharedPreferences.Editor ed2 = p2.edit();//MainActivity.xml
    SharedPreferences.Editor ed3 = p3.edit();//org.izv.aff.consultaagendaad
    SharedPreferences.Editor ed4 = p4.edit();//org.izv.aff.consultaagendaad
//Guardarlos
      ed1.putString("ved1", "v1");
                ed2.putString("ved2", "v2");
                ed3.putString("ved3", "v3");
                ed4.putString("ved4", "v4");
                ed1.commit();ed2.commit();ed3.commit();ed4.commit();*/
//Cuando necesite entrar en setting voy a necesitar utilizar si o si este PreferenceManager.getDefaultSharedPreferences(this)