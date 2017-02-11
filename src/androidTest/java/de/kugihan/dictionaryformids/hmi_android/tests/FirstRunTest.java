package de.kugihan.dictionaryformids.hmi_android.tests;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.kugihan.dictionaryformids.hmi_android.ChooseDictionary;
import de.kugihan.dictionaryformids.hmi_android.DictionaryForMIDs;
import de.kugihan.dictionaryformids.hmi_android.Preferences;
import de.kugihan.dictionaryformids.hmi_android.R;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class FirstRunTest {
    @Rule
    public IntentsTestRule<DictionaryForMIDs> mIntentsRule = new IntentsTestRule<DictionaryForMIDs>(
            DictionaryForMIDs.class,
            true,
            false);

    @Before
    public void setUp() {
        Context context = getInstrumentation().getTargetContext();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .remove(Preferences.PREF_VERSION)
                .commit();
    }

    @Test
    public void showWelcomeDialog() {
        mIntentsRule.launchActivity(new Intent());

        onView(withText(R.string.msg_first_run))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));

        onView(withId(android.R.id.button1))
                .perform(click());

        onView(withText(R.string.msg_first_run))
                .check(doesNotExist());

        intended(not(hasComponent(new ComponentName(getTargetContext(), DictionaryForMIDs.class))));
        intended(hasComponent(new ComponentName(getTargetContext(), ChooseDictionary.class)));
    }
}

