package ch.black_book.fhirconsent.LabelReader;

import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
        boolean foundCode = false;
        boolean foundDob = false;
        boolean tooManyLabels = false;

        PatientRecord theRecord = new PatientRecord();
        TextBlock nameBlock = null;
        TextBlock codeBlock = null;

        Rect localRect = new Rect();
        mGraphicOverlay.getLocalVisibleRect (localRect);
        mGraphicOverlay.clear();

        /*A bunch of image collection. Use this if scaling the image size
        int imgHeight = mGraphicOverlay.getHeight();
        float imgTranslationY = mGraphicOverlay.getTranslationY();
        int imgTop = mGraphicOverlay.getTop();
        int imgBot = mGraphicOverlay.getBottom();
        Rect globRect = new Rect();
        Point globOffset = new Point();
        int[] screenLoc = new int[2];
        int[] windowLoc = new int[2];
        mGraphicOverlay.getGlobalVisibleRect (globRect,globOffset);
        mGraphicOverlay.getLocationOnScreen(screenLoc);
        mGraphicOverlay.getLocationInWindow(windowLoc);*/

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

        SparseArray<TextBlock> items = detections.getDetectedItems();
        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);

            /*Log.d(logTag, "."
                    + "\nitem  top: " + item.getCornerPoints()[0].y
                    + "\nlocal top: " + localRect.top
                    + "\nitem  bot: " + item.getCornerPoints()[2].y
                    + "\nlocal bot: " + localRect.bottom);*/

            if (       (item.getCornerPoints()[0].y > localRect.top)        // item is lower than top
                    && (item.getCornerPoints()[2].y < localRect.bottom)     // item is higher than bottom
                    && (item.getCornerPoints()[0].x > localRect.left + 50)  // item is right of margin
                    && (item.getCornerPoints()[1].x < localRect.right - 50))// item is left of margin
            {

                //match with the name and DOB
                Pattern p = Pattern.compile("(\\w+[\\s]*[-\\w]*)[\\r\\n]+\\s*(\\w+[\\s]*[-\\w]*)[\\r\\n]+\\s*((0{0,1}[1-9]|[12][0-9]|3[01])[- \\/.](0{0,1}[1-9]|1[012])[- \\/.](\\d{4}))", 0);
                Matcher m = p.matcher(item.getValue());
                if (m.lookingAt()) {
                    if (foundName) {
                        tooManyLabels = true;
                    }
                    OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item);
                    mGraphicOverlay.add(graphic);

                    theRecord.LastName = m.group(1);
                    theRecord.FirstName = m.group(2);
                    theRecord.DateString = m.group(3);
                    //Log.d(logTag, "Name matched: " + theRecord.FirstName + " " + theRecord.LastName + " : " + theRecord.DateString);
                    foundName = true;
                    nameBlock = item;

                    // check if group 3 matches a Date
                    Calendar dobCalendar = Calendar.getInstance();
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
                    Date dob = null;
                    try {
                        dob = formatter.parse(m.group(3));
                        foundDob = true;
                    } catch (ParseException e) {
                        e.printStackTrace();
                        foundDob = false;
                    }
                    dobCalendar.setTime(dob);
                }

                //match with a code
                p = Pattern.compile("^(\\w+)$", 0);
                m = p.matcher(item.getValue());
                if (m.lookingAt()) {
                    if (foundCode) {
                        tooManyLabels = true;
                    }
                    OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item);
                    mGraphicOverlay.add(graphic);

                    theRecord.Code = m.group(1);
                    //Log.d(logTag, "Code matched: " + theRecord.Code);
                    foundCode = true;
                    codeBlock = item;
                }
            }
        }

        if (foundName && foundDob && foundCode && !tooManyLabels) {
            if (nameBlock.getCornerPoints()[0].y < codeBlock.getCornerPoints()[0].y) {
                Log.d(logTag, "patient detected: " + theRecord.FirstName + " " + theRecord.LastName + " " + theRecord.DateString + " : " + theRecord.Code);
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

