package ru.piter.fm.radio;

/**
 * Created by IntelliJ IDEA.
 * User: gb
 * Date: 26.08.2010
 * Time: 0:04:44
 * To change this template use File | SettingsActivity | File Templates.
 */
public class RadioFactory {

    public static final String PITER_FM = "PiterFM";
    public static final String MOSKVA_FM = "MoskvaFM";
    public static final String FAVOURITE = "Favourite";


    public static Radio getRadio(String name) {
        if (name.equals(PITER_FM))
            return new Radio(PITER_FM, "http://piter.fm");

        if (name.equals(MOSKVA_FM))
            return new Radio(MOSKVA_FM, "http://moskva.fm");

        if (name.equals(FAVOURITE))
            return new Radio(FAVOURITE, "");

        return null;
    }

}
