package ru.piter.fm.util;

import android.util.Log;
import ru.piter.fm.radio.Channel;
import ru.piter.fm.radio.Radio;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: gb
 * Date: 14.11.2010
 * Time: 15:53:30
 * To change this template use File | Settings | File Templates.
 */
public class RadioUtils {

    private static final String CHANNEL_HOST = "http://fresh.moskva.fm";
    private static final String PITER_FM_URL = "http://piter.fm/stations";
    private static final String MOSKVA_FM_URL = "http://moskva.fm/stations";
    private static final DateFormat DF = new SimpleDateFormat("yyyy/MM/dd/HHmm");
    private static final long TIME_MINUTE = 60000;

    public static String getTracksUrl(Date startAt, Channel channel) {
        String date = new SimpleDateFormat("yyyyMMdd").format(startAt);
        String url = channel.getRadio().getHostUrl() + "/station.xml.html?station=" + channel.getChannelId() + "&day=" + date;
        return url;

        //http://www.piter.fm/station.xml.html?station=7835&day=20101218&r=0.47836548276245594
    }


    public static Date getGMT4Date(Date currentDate, String timeZoneId) {
        // TimeZone tz = TimeZone.getTimeZone(timeZoneId);
        Calendar mbCal = new GregorianCalendar(TimeZone.getTimeZone("GMT+4"));
        mbCal.setTimeInMillis(currentDate.getTime());

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, mbCal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, mbCal.get(Calendar.MONTH));
        cal.set(Calendar.DAY_OF_MONTH, mbCal.get(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, mbCal.get(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, mbCal.get(Calendar.MINUTE));
        cal.set(Calendar.SECOND, mbCal.get(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, mbCal.get(Calendar.MILLISECOND));

        return cal.getTime();
    }


    public static int getTrackOffset(String time) {
        time = time.replaceAll(":", "/");
        String offset = time.substring(17, 19);
        return Integer.parseInt(offset);
    }

    public static String getTrackUrl(String time /*2010:11:13:20:31:12 */, String channelId) {
        time = time.replaceAll(":", "/");
        String currentTrack = time.substring(0, 11) + time.substring(11, 17).replaceAll("/", "");
        String url = CHANNEL_HOST + "/files/" + channelId + "/mp4/" + currentTrack + ".mp4";
        return url;
    }

    public static String getDuration(String durationInSeconds) {
        String minutes = String.valueOf(Integer.parseInt(durationInSeconds) / 60);
        int sec = Integer.parseInt(durationInSeconds) % 60;
        String seconds = String.valueOf(Integer.parseInt(durationInSeconds) % 60);
        if (sec < 10) seconds = "0" + seconds;
        return minutes + ":" + seconds;
    }

    public static List<Channel> getRadioChannels(Radio radio) throws Exception {
        List<Channel> channels = new ArrayList<Channel>();
        InputStream stream = Utils.openConnection(radio.getStationsUrl());
        Source source = new Source(stream);

        net.htmlparser.jericho.Element stationsList = source.getFirstElementByClass("msk-stations-block");
        //List<net.htmlparser.jericho.Element> stations = stationsList.getAllElements(HTMLElementName.DIV);
        List<net.htmlparser.jericho.Element> stations = stationsList.getChildElements();
        Log.d("PiterFM","stations = " + stations.size());
        Iterator iter = stations.iterator();
        while (iter.hasNext()) {
            try {
                net.htmlparser.jericho.Element div = (net.htmlparser.jericho.Element) iter.next();
                net.htmlparser.jericho.Element imgDiv =  div.getFirstElementByClass("thumbnail-img");
                net.htmlparser.jericho.Element captionDv = div.getFirstElementByClass("thumbnail-caption");
                net.htmlparser.jericho.Element a = imgDiv.getFirstElement(HTMLElementName.A);
                net.htmlparser.jericho.Element img = a.getFirstElement(HTMLElementName.IMG);


                Channel ch = new Channel();
                ch.setName(img.getAttributeValue("title"));
                String range = captionDv.getFirstElementByClass("meta").getContent().toString().replace("&nbsp;", " ");
                ch.setRange(range);
                String href = a.getAttributeValue("href");
                ch.setTranslationUrl(href);
                ch.setChannelId(href.split("/")[4]);
                ch.setLogoUrl(img.getAttributeValue("src"));
                ch.setRadio(radio);
                channels.add(ch);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        return channels;
    }


    public static String getTrackUrl(Channel channel) {
        String channelId = channel.getChannelId();
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/HHmm");
        Date date = getGMT4Date(new Date(System.currentTimeMillis() - (TIME_MINUTE * 5)), "Europe/Moscow");
        String currentTrack = dateFormat.format(date);
        String trackUrl = CHANNEL_HOST + "/files/" + channelId + "/mp4/" + currentTrack + ".mp4";
        Log.d("PiterFM","trackUrl = " + trackUrl);
        return trackUrl;
    }

    public static String getNextTrackUrl(String currentTrack) {
        String nextTrack = currentTrack;
        String array[] = currentTrack.split("/");
        String channelId = array[4];
        String year = array[6];
        String month = array[7];
        String day = array[8];
        String hs = (array[9]).split("\\.")[0];
        String hours = hs.substring(0, 2);
        String minutes = hs.substring(2, 4);

        try {
            Date date = DF.parse(year + "/" + month + "/" + day + "/" + hours + minutes);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MINUTE, 1);
            String track = DF.format(calendar.getTime());
            nextTrack = CHANNEL_HOST + "/files/" + channelId + "/mp4/" + track + ".mp4";
            return nextTrack;
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return nextTrack;

    }

    public static String getTrackNameFromUrl(String trackUrl) {
        if (trackUrl == null )return "";
        String[] a = trackUrl.split("/");
        return a[4] + "_" + a[5] + "_" + a[6] + "_" + a[7] + "_" + a[8] + "_" + a[9];
    }


}
