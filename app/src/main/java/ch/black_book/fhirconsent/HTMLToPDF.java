package ch.black_book.fhirconsent;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.print.PrintAttributes;
import android.print.pdf.PrintedPdfDocument;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.researchstack.backbone.result.TaskResult;
import org.researchstack.skin.task.ConsentTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class HTMLToPDF {
    public static void createPDFfromHTML(Context context, String consentHTML, String pathToPDF) {

        //change to webview
        // possible solution to blank webview: https://stackoverflow.com/questions/14300664/android-webview-displaying-blank-page


        if (Integer.valueOf(Build.VERSION.SDK_INT) >= 19) {

            int margin = 50;
            float textSize = 4;

            if (Integer.valueOf(Build.VERSION.SDK_INT) >= 26)
                textSize = 3.5f;
            else
                textSize = 6f;

            // open a new document
            PrintAttributes printAttributes = new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();

            PrintedPdfDocument document = new PrintedPdfDocument(context, printAttributes);

            // start a page
            PdfDocument.Page page = document.startPage(0);

            // draw something on the page
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(margin, margin, margin, margin);
            Log.d("LOG", "Layout Width,Height: " + layout.getWidth() + "," + layout.getHeight());

            // add a header to the pdf

            ImageView pdf_header = new ImageView(context);
            pdf_header.setAdjustViewBounds(true);
            pdf_header.setVisibility(View.VISIBLE);
            pdf_header.setImageResource(ch.usz.c3pro.c3_pro_android_framework.R.drawable.consent_pdf_header);
            pdf_header.setMaxWidth(page.getCanvas().getWidth()- margin*2);
            layout.addView(pdf_header);

            // add text
            TextView content = new TextView(context);



            content.setVisibility(View.VISIBLE);
            content.setWidth(page.getCanvas().getWidth()- margin*2);
            content.setTextSize(textSize);
            String[] stringParts = consentHTML.split("signature_here");

            if (Integer.valueOf(Build.VERSION.SDK_INT) >= 24) {
                content.setText(Html.fromHtml(stringParts[0], Html.FROM_HTML_MODE_COMPACT));
            } else{
                content.setText("pdf creation from html requires api level 24 or above");
            }
            layout.addView(content);
            Log.d("LOG", "Content  Width,Height: " + content.getWidth() + "," + content.getHeight());


            RelativeLayout relLayout = new RelativeLayout(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 2f);
            relLayout.setLayoutParams(layoutParams);
            relLayout.setGravity(Gravity.LEFT);


            if (stringParts.length > 1){
                TextView content2 = new TextView(context);
                content2.setVisibility(View.VISIBLE);
                content2.setWidth(page.getCanvas().getWidth()- margin*2);
                Log.d("LOG", "Content2 Width,Height: " + content2.getWidth() + "," + content2.getHeight());
                content2.setTextSize(new Float(textSize));
                if (Integer.valueOf(Build.VERSION.SDK_INT) >= 24) {
                    content2.setText(Html.fromHtml(stringParts[1], Html.FROM_HTML_MODE_COMPACT));
                } else{
                    content2.setText("pdf creation from html requires api level 24 or above");
                }
                layout.addView(content2);
            }

            Log.d("LOG", "Canvas Width,Height: " + page.getCanvas().getWidth() + "," + page.getCanvas().getHeight());
            layout.measure(page.getCanvas().getWidth(), page.getCanvas().getHeight());
            layout.layout(0, 0, page.getCanvas().getWidth(), page.getCanvas().getHeight());
            Log.d("LOG", "Layout Width,Height: " + layout.getWidth() + "," + layout.getHeight());

            layout.draw(page.getCanvas());


            // finish the page
            document.finishPage(page);

            // add more pages

            // write the document content
            try {
                //String outpath = Environment.getExternalStorageDirectory() + "/consent§.pdf";
                OutputStream file = new FileOutputStream(new File(pathToPDF));


                document.writeTo(file);

                //close the document
                document.close();
                Log.d("LOG", "pdf saved to " + pathToPDF);


            } catch (IOException e) {
                Log.d("error", "testPdf: " + e);
            }
        }
    }

}