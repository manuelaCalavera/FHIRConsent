package ch.black_book.fhirconsent.LabelReader;


import android.util.Log;
import android.util.SparseArray;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;

import static ch.black_book.fhirconsent.MainActivity.logTag;


/**
 * A very simple Processor which receives detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {

    private OcrDetectorListener listener;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;

    OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay, OcrDetectorListener ocrDetectorListener) {
        listener = ocrDetectorListener;
        mGraphicOverlay = ocrGraphicOverlay;
    }

    /**
     * Called by the detector to deliver detection results.
     * If your application called for it, this could be a place to check for
     * equivalent detections by tracking TextBlocks that are similar in location and content from
     * previous frames, or reduce noise by eliminating TextBlocks that have not persisted through
     * multiple detections.
     */
    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        boolean foundName = false;
        boolean foundCode= false;
        boolean tooManyLabels = false;

        PatientRecord theRecord = new PatientRecord();
        TextBlock nameBlock = null;
        TextBlock codeBlock = null;

        /**
         * Checking the TextBlocks to match the USZ small patient label
         * __________________________________
         *|                                 |
         *| Lastname                        |
         *| Firstname                       |
         *| DOB                             |
         *|                                 |
         *| Patient ID                      |
         *|_________________________________|
         * */
        mGraphicOverlay.clear();
        SparseArray<TextBlock> items = detections.getDetectedItems();
        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            Pattern p = Pattern.compile("(\\w+[\\s]*[-\\w]*)[\\r\\n]+\\s*(\\w+[\\s]*[-\\w]*)[\\r\\n]+\\s*((0{0,1}[1-9]|[12][0-9]|3[01])[- \\/.](0{0,1}[1-9]|1[012])[- \\/.](\\d{4}))",0);
            Matcher m = p.matcher( item.getValue() );

            if (m.lookingAt()) {
                if (foundName) {
                    tooManyLabels = true;
                }

                OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item);

                mGraphicOverlay.add(graphic);

                theRecord.LastName = m.group(1);
                theRecord.FirstName = m.group(2);
                theRecord.DateString = m.group(3);

                Log.d(logTag, "Name matched: "+theRecord.FirstName + " " + theRecord.LastName + " : " + theRecord.DateString);

                foundName = true;
                nameBlock = item;
            }

            p = Pattern.compile("^(\\w+)$",0);
            m = p.matcher( item.getValue() );

            if (m.lookingAt()) {
                if (foundCode) {
                    tooManyLabels = true;
                }

                OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item);
                mGraphicOverlay.add(graphic);

                theRecord.Code = m.group(1);

                Log.d(logTag, "Code matched: "+theRecord.Code);

                foundCode = true;
                codeBlock = item;
            }
        }

        if (foundName && foundCode && !tooManyLabels)
        {
            if(nameBlock.getCornerPoints()[0].y < codeBlock.getCornerPoints()[0].y) {
                Log.d(logTag, "patient detected: "+theRecord.FirstName + " " + theRecord.LastName + " " + theRecord.DateString + " : " + theRecord.Code);
                listener.onPatientDetected(theRecord);
            }
        }
    }



    /**
     * Frees the resources associated with this detection processor.
     */
    @Override
    public void release() {
        mGraphicOverlay.clear();
    }
}

