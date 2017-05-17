package dhbw.timetable.data.logic;

import android.util.Pair;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import dhbw.timetable.data.Appointment;

/**
 * Created by Hendrik Ulbrich (C) 2017
 */
public final class DateHelper {

    private DateHelper() {}

    public static boolean IsDateOver(GregorianCalendar date, GregorianCalendar isOver) {
        if(date.get(Calendar.YEAR) == isOver.get(Calendar.YEAR)) {
            if(date.get(Calendar.MONTH) == isOver.get(Calendar.MONTH)) {
                if(date.get(Calendar.DAY_OF_MONTH) == isOver.get(Calendar.DAY_OF_MONTH)) {
                    return false;
                } else {
                    return date.get(Calendar.DAY_OF_MONTH) > isOver.get(Calendar.DAY_OF_MONTH);
                }
            } else {
                return date.get(Calendar.MONTH) > isOver.get(Calendar.MONTH);
            }
        } else {
            return date.get(Calendar.YEAR) > isOver.get(Calendar.YEAR);
        }
    }

    public static void NextWeek(GregorianCalendar g) {
        AddDays(g, 7);
        Normalize(g);
    }

    public static String GetCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        Calendar c = Calendar.getInstance();
        return sdf.format(c.getTime());
    }

    public static boolean IsSameWeek(GregorianCalendar g1, GregorianCalendar g2) {
        return g1.get(Calendar.WEEK_OF_YEAR) == g2.get(Calendar.WEEK_OF_YEAR)
                && g1.get(Calendar.YEAR) == g2.get(Calendar.YEAR);
    }

    /**
     * Normalizes the day to a week starting with monday
     */
    public static void Normalize(GregorianCalendar g) {
        g.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
    }

    public static void AddDays(GregorianCalendar g, int i) {
        g.add(Calendar.DAY_OF_MONTH, i);
    }

    public static void SubtractDays(GregorianCalendar g, int i) {
        g.add(Calendar.DAY_OF_YEAR, -i);
    }

    public static Appointment GetFirstAppointmentOfDay(ArrayList<Appointment> appointments, GregorianCalendar day) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        for(Appointment a : appointments) {
            // If same day
            if(sdf.format(a.getStartDate().getTime()).equals(sdf.format(day.getTime()))) {
                // Since appointments are sorted -> first element is a match
                return a;
            }
        }
        return null;
    }

    public static Appointment GetLastAppointmentOfDay(ArrayList<Appointment> appointments, GregorianCalendar day) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        Appointment result = null;
        for(Appointment a : appointments) {
            // If same day
            if(sdf.format(a.getStartDate().getTime()).equals(sdf.format(day.getTime()))) {
                // Since appointments are sorted -> first element is a match
                result = a;
            }
        }
        return result;
    }

    public static Pair<Integer, Integer> GetBorders(ArrayList<Appointment> weekAppointments) {
        int startOnMin, endOnMin, max = 0, min = 1440;
        for(Appointment a : weekAppointments) {
            startOnMin = a.getStartDate().get(Calendar.HOUR_OF_DAY) * 60
                        + a.getStartDate().get(Calendar.MINUTE);
            endOnMin = a.getEndDate().get(Calendar.HOUR_OF_DAY) * 60
                    + a.getEndDate().get(Calendar.MINUTE);
            if(startOnMin < min) {
                min = startOnMin;
            }
            if(endOnMin > max) {
                max = endOnMin;
            }
        }
        return new Pair<>(min, max);
    }

    public static ArrayList<Appointment> GetWeekAppointments(GregorianCalendar day, ArrayList<Appointment> superList) {
        ArrayList<Appointment> weekAppointments = new ArrayList<>();
        for(Appointment a : superList) {
            if(DateHelper.IsSameWeek(a.getStartDate(), day)) {
                weekAppointments.add(a);
            }
        }
        return weekAppointments;
    }



    public static ArrayList<Appointment> GetAppointmentsOfDay(GregorianCalendar day, ArrayList<Appointment> list) {
        ArrayList<Appointment> dayAppointments = new ArrayList<>();
        String currDate = new SimpleDateFormat("dd.MM.yyyy").format(day.getTime());
        for(Appointment a : list) {
            if(a.getDate().equals(currDate)) {
                dayAppointments.add(a);
            }
        }
        return dayAppointments;
    }
}
