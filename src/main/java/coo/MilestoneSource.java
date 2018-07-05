package coo;

import com.google.common.base.Strings;
import jota.model.Transaction;

import java.util.List;

public abstract class MilestoneSource {
    public final static String EMPTY_HASH = Strings.repeat("9", 81);
    public final static String EMPTY_TAG = Strings.repeat("9", 27);
    public final static String EMPTY_MSG = Strings.repeat("9", 27 * 81);

    public abstract String getRoot();

    public abstract List<Transaction> createMilestone(String trunk, String branch, int index, int mwm);
}
