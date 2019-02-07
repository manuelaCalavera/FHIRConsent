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
import android.util.Log;
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

import ch.black_book.fhirconsent.LabelReader.OcrCaptureActivity;
import ch.black_book.fhirconsent.LabelReader.PatientRecord;
import ch.usz.c3pro.c3_pro_android_framework.consent.ViewConsentTaskActivity;
import ch.usz.c3pro.c3_pro_android_framework.pyromaniac.Pyro;
import ch.usz.c3pro.c3_pro_android_framework.pyromaniac.logic.consent.ConsentTaskOptions;
import ch.usz.c3pro.c3_pro_android_framework.pyromaniac.logic.consent.ContractAsTask;
import ch.usz.c3pro.c3_pro_android_framework.pyromaniac.logic.consent.CreateConsentPDF;

public class MainActivity extends AppCompatActivity {

    public static final String logTag = "MY_LOG";
    private static String contractFilePath = "contract.json";
    private static int GET_CONSENT = 1;
    private static int RC_OCR_CAPTURE = 2;
    private static int CHECK_INFO = 3;
    private ConsentTaskOptions consentTaskOptions;
    private PatientRecord patientRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {

                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            777);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               readLabel();
            }
        });

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

                createPDF(result);

            }
        }
        if (requestCode == RC_OCR_CAPTURE){
            Log.d(logTag, "BOTH MATCHED: request code matched");
            if (resultCode == RESULT_OK) {
                Log.d(logTag, "BOTH MATCHED: result ok");

                PatientRecord rec = new PatientRecord();

                rec.FirstName = data.getStringExtra("FIRST_NAME");
                rec.LastName = data.getStringExtra("LAST_NAME");
                rec.Code = data.getStringExtra("CODE");
                rec.DateString = data.getStringExtra("DOB");
                Log.d(logTag, "I received the patient from OCR_CAPTURE!");
                patientRecord = rec;
                Log.d(logTag, patientRecord.FirstName+" "+patientRecord.LastName+" "+patientRecord.DateString+" "+patientRecord.Code);

                checkInfo();
            }
        }
        if (requestCode == CHECK_INFO){
            if (resultCode == RESULT_OK) {

                PatientRecord rec = new PatientRecord();

                rec.FirstName = data.getStringExtra("FIRST_NAME");
                rec.LastName = data.getStringExtra("LAST_NAME");
                rec.Code = data.getStringExtra("CODE");
                rec.DateString = data.getStringExtra("DOB");
                patientRecord = rec;
                Log.d(logTag, "I checked the patient!");
                Log.d(logTag, patientRecord.FirstName+" "+patientRecord.LastName+" "+patientRecord.DateString+" "+patientRecord.Code);

                startConsent();
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

    private void createPDF(TaskResult result) {
        // create consent pdf

        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
        Date now = Calendar.getInstance().getTime();
        String today = formatter.format(now);

        String contractString = ResourcePathManager.getResourceAsString(getApplicationContext(), "html/consentPDFcontent.html");
        contractString = contractString.replace("$probenentnahme$", "Testprobe");
        contractString = contractString.replace("$institution$", "USZ");
        contractString = contractString.replace("$name$", patientRecord.FirstName + " " + patientRecord.LastName);
        contractString = contractString.replace("$dob$", patientRecord.DateString);
        contractString = contractString.replace("$datum$", today);


        CreateConsentPDF.createPDFfromHTML(this, contractString, result, Environment.getExternalStorageDirectory()+"/consent.pdf");
    }

    private void readLabel(){
        Intent intent = new Intent (this, OcrCaptureActivity.class);
        intent.putExtra(OcrCaptureActivity.AutoFocus, true);
        intent.putExtra(OcrCaptureActivity.UseFlash, false);

        startActivityForResult(intent, RC_OCR_CAPTURE);
    }

    private void checkInfo(){
        Intent intent = new Intent (this, CheckInfoActivity.class);
        intent.putExtra("FIRST_NAME", patientRecord.FirstName);
        intent.putExtra("LAST_NAME", patientRecord.LastName);
        intent.putExtra("CODE", patientRecord.Code);
        intent.putExtra("DOB", patientRecord.DateString);

        startActivityForResult(intent, CHECK_INFO);
    }

    private void startConsent() {
        String contractString = ResourcePathManager.getResourceAsString(getApplicationContext(), contractFilePath);
        contractString = contractString.replace("$probenentnahme$", "Testprobe");
        contractString = contractString.replace("$institution$", "USZ");
        final Contract contract = Pyro.getFhirContext().newJsonParser().parseResource(Contract.class, contractString);

        consentTaskOptions = new ConsentTaskOptions();
        consentTaskOptions.setRequiresBirthday(false);
        consentTaskOptions.setRequiresName(false);
        consentTaskOptions.setReviewConsentDocument("consent");
        consentTaskOptions.setAskForSharing(false);

        Intent intent = ViewConsentTaskActivity.newIntent(getApplicationContext(), contract, consentTaskOptions);

        startActivityForResult(intent, GET_CONSENT);
    }

    private void writeSignatureToView(TaskResult result) {
        String signatureEncodeBase64 = (String) result.getStepResult(ConsentTask.ID_SIGNATURE).getResultForIdentifier("ConsentSignatureStep.Signature");

        byte[] decodedString = Base64.decode(signatureEncodeBase64, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        ImageView signatureView = (ImageView) findViewById(R.id.signature_view);
        signatureView.setImageBitmap(decodedByte);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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
            case 777: {
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
