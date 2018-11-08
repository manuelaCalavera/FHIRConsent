package ch.black_book.fhirconsent;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.hl7.fhir.dstu3.model.Contract;
import org.researchstack.backbone.ResourcePathManager;
import org.researchstack.backbone.result.StepResult;
import org.researchstack.backbone.result.TaskResult;
import org.researchstack.skin.task.ConsentTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ch.usz.c3pro.c3_pro_android_framework.consent.ViewConsentTaskActivity;
import ch.usz.c3pro.c3_pro_android_framework.pyromaniac.Pyro;
import ch.usz.c3pro.c3_pro_android_framework.pyromaniac.logic.consent.ConsentTaskOptions;
import ch.usz.c3pro.c3_pro_android_framework.pyromaniac.logic.consent.ContractAsTask;
import ch.usz.c3pro.c3_pro_android_framework.pyromaniac.logic.consent.CreateConsentPDF;

public class MainActivity extends AppCompatActivity {

    private static String contractFilePath = "contract.json";
    private static int GET_CONSENT = 1;
    private ConsentTaskOptions consentTaskOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String contractString = ResourcePathManager.getResourceAsString(getApplicationContext(), contractFilePath);
        final Contract contract = Pyro.getFhirContext().newJsonParser().parseResource(Contract.class, contractString);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                consentTaskOptions = new ConsentTaskOptions();
                consentTaskOptions.setRequiresBirthday(true);
                consentTaskOptions.setRequiresName(true);
                consentTaskOptions.setReviewConsentDocument("consent");
                consentTaskOptions.setAskForSharing(false);

                Intent intent = ViewConsentTaskActivity.newIntent(getApplicationContext(), contract, consentTaskOptions);

                startActivityForResult(intent, GET_CONSENT);
            }
        });

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        666);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request it is that we're responding to
        if (requestCode == GET_CONSENT) {
            if (resultCode == RESULT_OK) {

                // write signature to view
                TaskResult result = (TaskResult) data.getSerializableExtra(ViewConsentTaskActivity.EXTRA_TASK_RESULT);
                writeSignatureToView(result);

                // create consent pdf

                StepResult nameResult = (StepResult) result.getStepResult(ContractAsTask.ID_FORM).getResultForIdentifier(ContractAsTask.ID_FORM_NAME);
                String name = (String) nameResult.getResult();
                
                StepResult dobResult = (StepResult) result.getStepResult(ContractAsTask.ID_FORM).getResultForIdentifier(ContractAsTask.ID_FORM_DOB);
                Date dob = new Date((long) dobResult.getResult());
                SimpleDateFormat formatter = new SimpleDateFormat("dd.mm.yyyy");
                String birthday = formatter.format(dob);

                Date now = Calendar.getInstance().getTime();
                String today = formatter.format(now);

                String contractString = ResourcePathManager.getResourceAsString(getApplicationContext(), "html/consentPDFcontent.html");
                contractString = contractString.replace("$probenentnahme$", "Testprobe");
                contractString = contractString.replace("$institution$", "USZ");
                contractString = contractString.replace("$name$", name);
                contractString = contractString.replace("$dob$", birthday);
                contractString = contractString.replace("$datum$", birthday);




                CreateConsentPDF.createPDFfromHTML(this, contractString, result, Environment.getExternalStorageDirectory()+"/consent.pdf");

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void writeSignatureToView(TaskResult result) {
        String signatureEncodeBase64 = (String) result.getStepResult(ConsentTask.ID_SIGNATURE).getResultForIdentifier("ConsentSignatureStep.Signature");

        byte[] decodedString = Base64.decode(signatureEncodeBase64, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        ImageView signatureView = (ImageView) findViewById(R.id.signature_view);
        signatureView.setImageBitmap(decodedByte);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 666: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
