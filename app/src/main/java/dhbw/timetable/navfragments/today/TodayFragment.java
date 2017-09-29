package dhbw.timetable.navfragments.today;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.Locale;

import dhbw.timetable.MainActivity;
import dhbw.timetable.R;
import dhbw.timetable.data.AgendaAppointment;
import dhbw.timetable.data.Appointment;
import dhbw.timetable.data.DateHelper;
import dhbw.timetable.data.ErrorCallback;
import dhbw.timetable.data.TimelessDate;
import dhbw.timetable.data.Timetable;
import dhbw.timetable.data.TimetableManager;
import dhbw.timetable.dialogs.ErrorDialog;
import dhbw.timetable.navfragments.week.WeekFragment;
import dhbw.timetable.views.TodaySummaryRect;

/**
 * Created by Hendrik Ulbrich (C) 2017
 */
public class TodayFragment extends Fragment {
    RecyclerView recyclerView;
    private AgendaAppointmentAdapter aAdapter;
    private LinkedHashSet<AgendaAppointment> agendaAppointmentSet = new LinkedHashSet<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //getActivity().setTitle("Today");
        TextView actTitle = (TextView) getActivity().findViewById(R.id.toolbar_title);
        actTitle.setText("Today");
        actTitle.setOnClickListener(null);
        Log.d("TODAY", "onViewCreated");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AppBarLayout appBarLayout = (AppBarLayout) getActivity().findViewById(R.id.appbar);

        if (appBarLayout.getChildCount() != 1) {
            appBarLayout.removeViewAt(1);
        }

        final View view = inflater.inflate(R.layout.content_today, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclingAgenda);

        aAdapter = new AgendaAppointmentAdapter(agendaAppointmentSet);
        RecyclerView.LayoutManager aLayoutManager = new LinearLayoutManager(view.getContext()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        recyclerView.setLayoutManager(aLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(aAdapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        final TimelessDate today = new TimelessDate();
        final TimelessDate week = new TimelessDate();
        DateHelper.Normalize(week);
        final View view = getView();
        TimetableManager.getInstance().loadOfflineGlobals(getActivity().getApplication(), new Runnable() {
            @Override
            public void run() {
                ArrayList appointments = DateHelper.GetAppointmentsOfDay(today, TimetableManager.getInstance().getGlobals().get(week));
                if(appointments.size() == 0) {
                    Log.w("TODAY", "Warning: No appointments found for day.");
                    TimetableManager.getInstance().updateGlobals(TodayFragment.this.getActivity().getApplication(), new Runnable() {
                        @Override
                        public void run() {
                            if(view != null) {
                                applyGlobalContent(view);
                            } else {
                                Log.w("TODAY", "WARNING: Today tried to start without view. (Too early)");
                            }
                        }
                    }, new ErrorCallback() {
                        @Override
                        public void onError(String string) {
                            ErrorDialog.newInstance("Error", "Sync lag. You have no internet connection and your offline data is not up to date.", string);
                        }
                    });
                } else {
                    applyGlobalContent(view);
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_today, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        final View view = getView();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh_today) {
            if(!TimetableManager.getInstance().isRunning()) {
                TimetableManager.getInstance().loadOfflineGlobals(getActivity().getApplication(), new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if(view != null) {
                                applyGlobalContent(view);
                            } else {
                                Log.w("TODAY", "WARNING: Today tried to select option without view. (Too early)");
                            }
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                });
                TimetableManager.getInstance().updateGlobals(getActivity().getApplication(), new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (view != null) {
                                applyGlobalContent(view);
                                Snackbar.make(view, "Updated!", Snackbar.LENGTH_SHORT).show();
                            } else {
                                Log.w("TODAY", "WARNING: Today tried to select option without view. (Too early)");
                            }
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                }, new ErrorCallback() {
                    @Override
                    public void onError(String string) {
                        ErrorDialog.newInstance("Error", "Unable to update timetable data", string)
                                .show(TodayFragment.this.getActivity().getFragmentManager(), "TODAYDLERR");
                    }
                });
                return true;
            } else {
                Log.w("ASYNC", "Tried to sync while manager was busy");
                Toast.makeText(getContext(), "I'm currently busy, sorry!", Toast.LENGTH_SHORT).show();
            }
        }

        return false;
    }

    public void applyGlobalContent(View view) {
        try {
            applyAgenda(view);
            applyTomorrow(view);
            applyWeekSummary(view);
        } catch(IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void applyAgenda(View view) {
        agendaAppointmentSet.clear();
        String currDate = DateHelper.GetCurrentDate();
        for(Appointment a : TimetableManager.getInstance().getGlobalsAsList()) {
            if(a.getDate().equals(currDate)) {
                agendaAppointmentSet.add(new AgendaAppointment(a.getStartTime(), a.getEndTime(), a.getCourse(), a.getInfo(), false));
            }
        }
        int size = agendaAppointmentSet.size();
        TextView placeholder = (TextView) view.findViewById(R.id.agendaEmptyPlaceholder);
        if(size > 0) {
            placeholder.setText("");
            LinkedHashSet<AgendaAppointment> appointmentsWithBreaks = new LinkedHashSet<>();

            Object[] agendaAppointmentArray = agendaAppointmentSet.toArray();

            for (int i = 0; i < size; i++) {
                AgendaAppointment aa = (AgendaAppointment) agendaAppointmentArray[i];
                appointmentsWithBreaks.add(aa);
                if(i < size - 1) {
                    AgendaAppointment following = (AgendaAppointment) agendaAppointmentArray[i + 1];
                    // If break is present
                    if (!aa.getEndTime().equals(following.getStartTime())) {
                        appointmentsWithBreaks.add(new AgendaAppointment(aa.getEndTime(), "DONOTUSE", "BREAK", "DONOTUSE", true));
                    }
                }
            }
            agendaAppointmentSet.clear();
            agendaAppointmentSet.addAll(appointmentsWithBreaks);

            String endTime = ((AgendaAppointment) agendaAppointmentArray[agendaAppointmentArray.length -1]).getEndTime();
            agendaAppointmentSet.add(new AgendaAppointment(endTime, "", "END", "DONOTUSE", true));

            aAdapter.notifyDataSetChanged();
        } else {
            placeholder.setText("No appointments");
        }
    }

    private void applyTomorrow(View view) {
        TimelessDate tomorrow = new TimelessDate();
        DateHelper.AddDays(tomorrow, 1);
        LinkedHashSet<Appointment> tomorrowAppointments = DateHelper.GetAppointmentsOfDayAsSet(tomorrow,
                TimetableManager.getInstance().getGlobalsAsSet());
        TextView beginView = (TextView) view.findViewById(R.id.beginTime);
        TextView tomorrowSummaryView = (TextView) view.findViewById(R.id.tomorrowSummary);
        if(tomorrowAppointments.size() > 0) {
            final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.GERMANY);
            final SharedPreferences sharedPref = getActivity().getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);

            GregorianCalendar startDate = ((Appointment) tomorrowAppointments.toArray()[0]).getStartDate();
            int shiftInMillis = ((60 * sharedPref.getInt("alarmFirstShiftHour", 0))
                    + sharedPref.getInt("alarmFirstShiftMinute", 0)) * 60 * 1000;

            String alarm;

            if (sharedPref.getBoolean("alarmOnFirstEvent", false)) {
                alarm = shiftInMillis > 0 ? timeFormat.format(startDate.getTimeInMillis() - shiftInMillis) : "Immediately";
            } else {
                alarm = "None";
            }

            beginView.setText("Alarm: " + alarm + "\n" + "Begin: " + timeFormat.format(startDate.getTime()));
            StringBuilder sb = new StringBuilder();
            for(Appointment a : tomorrowAppointments) sb.append(a.getCourse()).append(",\n");
            // Delete last comma
            sb.deleteCharAt(sb.length() - 2);
            tomorrowSummaryView.setText(sb.toString());
        } else {
            beginView.setText("No appointments");
            tomorrowSummaryView.setText("");
        }
    }

    private void applyWeekSummary(final View view) {
        TextView weekHeadline = (TextView) view.findViewById(R.id.weekHeadline);
        TimelessDate day = new TimelessDate();

        int iDay = day.get(Calendar.DAY_OF_WEEK);
        if(iDay == Calendar.SATURDAY || iDay == Calendar.SUNDAY) {
            DateHelper.NextWeek(day);
            weekHeadline.setText("Next week");
        } else {
            weekHeadline.setText("Week");
        }
        DateHelper.Normalize(day);

        ArrayList<Appointment> weekAppointments = DateHelper.GetWeekAppointments(day,
                TimetableManager.getInstance().getGlobalsAsList());

        String startTime, endTime;
        int startID = -1, endID = -1;
        Appointment startA, endA;
        ArrayList<ArrayList<Appointment>> wData = new ArrayList<>();
        for(int d = 0; d < 5; d++) {
            startA = endA = null;
            ArrayList<Appointment> dayAppointments = DateHelper.GetAppointmentsOfDay(day, weekAppointments);
            wData.add(dayAppointments);
            if (dayAppointments.size() > 0){
                startA = dayAppointments.get(0);
                endA = dayAppointments.get(dayAppointments.size() - 1);
            }
            switch (d) {
                case 0:
                    startID = R.id.moStart;
                    endID = R.id.moEnd;
                    break;
                case 1:
                    startID = R.id.diStart;
                    endID = R.id.diEnd;
                    break;
                case 2:
                    startID = R.id.miStart;
                    endID = R.id.miEnd;
                    break;
                case 3:
                    startID = R.id.doStart;
                    endID = R.id.doEnd;
                    break;
                case 4:
                    startID = R.id.frStart;
                    endID = R.id.frEnd;
                    break;
            }
            if (startA != null) {
                startTime = startA.getStartTime();
                endTime = endA.getEndTime();
            } else {
                startTime = endTime = "";
            }
            ((TextView) view.findViewById(startID)).setText(startTime);
            ((TextView) view.findViewById(endID)).setText(endTime);

            DateHelper.AddDays(day, 1);
        }
        GridLayout gl = (GridLayout) view.findViewById(R.id.weekGrid);
        gl.removeAllViews();
        Pair<Integer, Integer> borders = DateHelper.GetBorders(weekAppointments);
        TodaySummaryRect ra = new TodaySummaryRect(borders.first, borders.second, gl, wData);
        ra.setBackgroundColor(Color.WHITE);
        ra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!TimetableManager.getInstance().isRunning()) {
                    ((MainActivity) getActivity()).displayFragment(R.id.nav_week);
                    NavigationView navigationView = (NavigationView) TodayFragment.this.getActivity().findViewById(R.id.nav_view);
                    navigationView.setCheckedItem(R.id.nav_week);
                } else {
                    Toast.makeText(getActivity(), "I'm currently busy, sorry!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        gl.addView(ra);
    }
}
