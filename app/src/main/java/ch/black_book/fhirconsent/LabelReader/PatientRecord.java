package ch.black_book.fhirconsent.LabelReader;


import java.util.Date;

public class PatientRecord {
    public String LastName = "";
    public String FirstName = "";
    public String DateString = "";
    //public Date DateOfBirth = new Date(0);
    public String Code = "";

    public PatientRecord()
    {
        LastName ="";
        FirstName = "";
        DateString = "";
        //DateOfBirth.setTime(0);
        Code = "";
    }
}