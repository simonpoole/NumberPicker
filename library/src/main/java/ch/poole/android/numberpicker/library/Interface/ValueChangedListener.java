package ch.poole.android.numberpicker.library.Interface;

import ch.poole.android.numberpicker.library.Enums.ActionEnum;

/**
 * Created by travijuu on 19/12/16.
 */

public interface ValueChangedListener {

    void valueChanged(int value, ActionEnum action);
}
