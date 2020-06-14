package de.jost_net.JVerein.util;

import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.qrcode.EncodeHintType;
import com.itextpdf.text.pdf.qrcode.ErrorCorrectionLevel;

import de.jost_net.JVerein.Einstellungen;

public class EpcQrCode extends BarcodeQRCode {
    public EpcQrCode(double amount, String purpose, String reference, String text, String userNote) {
        super(composeData(0, "", "", "", ""), codeSize, codeSize, QR_PARAM);
    }

    public EpcQrCode(double amount, String text) {
        super(composeData(amount, "", "", text, ""), codeSize, codeSize, QR_PARAM);
    }

    public EpcQrCode() {
        super(composeData(0, "", "", "", ""), codeSize, codeSize, QR_PARAM);
    }

    public static class DataElements {
        static final String serviceTag = "BCD";
        static final String version = "001";
        static final String coding = "2";
        static final String function = "SCT";
        static String bic = "";
        static String receiver = "";
        static String iban = "";
        String amount;
        String purpose;
        String reference;
        String text;
        String display;
    };

    public static final Integer MAXLENGTH_PURPOSE = 4;
    public static final Integer MAXLENGTH_REFERENCE = 35;
    public static final Integer MAXLENGTH_TEXT = 140;
    public static final Integer MAXLENGTH_DISPLAY = 70;
    public static String textDelimiter = " + ";
    public static Integer codeSize = 140;
    public static String charset = "ISO-8859-1";

    private static final Map<EncodeHintType, Object> QR_PARAM = createMap();

    private static Map<EncodeHintType, Object> createMap() {
        Map<EncodeHintType, Object> result = new HashMap<>();
        result.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        result.put(EncodeHintType.CHARACTER_SET, charset);
        return Collections.unmodifiableMap(result);
    }

    public static String concatText(ArrayList<String> textList) {
        return String.join(textDelimiter, textList);
    }

    public static void setDefaultReceiverAccount(String receiverName, String iban, String bic) {
        DataElements.receiver = receiverName;
        DataElements.iban = iban;
        DataElements.bic = bic;
    }

    private static String composeData(double amount, String purpose, String reference, String text, String userNote)
    {
        if (DataElements.bic.isEmpty() || DataElements.receiver.isEmpty() || DataElements.iban.isEmpty()) 
        {
            querySystemReceiverAccount();
        }

        if (!reference.isEmpty() && !text.isEmpty())
        {
            // both fields are mutually exclusive, prefer reference data
            text = "";
        }

        DataElements codeContent = new DataElements();
        codeContent.amount = formatAmount(amount);
        codeContent.text = replaceUmlaut(trimText(text, MAXLENGTH_TEXT));
        codeContent.purpose = replaceUmlaut(trimText(purpose, MAXLENGTH_PURPOSE));
        codeContent.reference = replaceUmlaut(trimText(reference, MAXLENGTH_REFERENCE));
        codeContent.display = replaceUmlaut(trimText(userNote, MAXLENGTH_DISPLAY));

        return composeCodeContent(codeContent);
    }

    private static void querySystemReceiverAccount()
    {
        try {
            DataElements.iban = Einstellungen.getEinstellung().getIban();
            DataElements.bic = Einstellungen.getEinstellung().getBic();
            DataElements.receiver = replaceUmlaut(Einstellungen.getEinstellung().getName());
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static String formatAmount(double amount)
    {
        if (amount > 0.0)
        {
            DecimalFormat df = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.US));
            df.setMaximumIntegerDigits(9);
            String formattedAmount = df.format(amount);
            return "EUR" + formattedAmount;
        }
        else
        {
            return "";
        }
    }

    private static String trimText(String text, Integer maxLength)
    {
        return text.substring(0, Math.min(maxLength, text.length()));
    }

    private static String replaceUmlaut(String input) {
        String output = input.replace("ü", "ue")
                             .replace("ö", "oe")
                             .replace("ä", "ae")
                             .replace("ß", "ss");
   
        output = output.replace("Ü", "Ue")
                       .replace("Ö", "Oe")
                       .replace("Ä", "Ae");
   
        return output;
    }

    private static String composeCodeContent(final DataElements codeContent)
    {
        return String.join("\n", 
            DataElements.serviceTag,
            DataElements.version,
            DataElements.coding,
            DataElements.function,
            DataElements.bic,
            DataElements.receiver,
            DataElements.iban,
            codeContent.amount,
            codeContent.purpose,
            codeContent.reference,
            codeContent.text,
            codeContent.display
        );
    }
}