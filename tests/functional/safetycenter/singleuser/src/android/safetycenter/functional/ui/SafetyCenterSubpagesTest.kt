/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.safetycenter.functional.ui

import android.content.Context
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import android.os.Bundle
import android.safetycenter.SafetyCenterManager.EXTRA_SAFETY_SOURCES_GROUP_ID
import android.safetycenter.SafetySourceData
import android.safetycenter.config.SafetySource
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import com.android.compatibility.common.util.DisableAnimationRule
import com.android.compatibility.common.util.FreezeRotationRule
import com.android.compatibility.common.util.UiAutomatorUtils2
import com.android.safetycenter.testing.SafetyCenterActivityLauncher.launchSafetyCenterActivity
import com.android.safetycenter.testing.SafetyCenterFlags
import com.android.safetycenter.testing.SafetyCenterFlags.deviceSupportsSafetyCenter
import com.android.safetycenter.testing.SafetyCenterTestConfigs
import com.android.safetycenter.testing.SafetyCenterTestConfigs.Companion.MULTIPLE_SOURCES_GROUP_ID_1
import com.android.safetycenter.testing.SafetyCenterTestConfigs.Companion.PRIVACY_SOURCE_ID_1
import com.android.safetycenter.testing.SafetyCenterTestConfigs.Companion.PRIVACY_SOURCE_ID_2
import com.android.safetycenter.testing.SafetyCenterTestConfigs.Companion.SINGLE_SOURCE_ID
import com.android.safetycenter.testing.SafetyCenterTestConfigs.Companion.SOURCE_ID_1
import com.android.safetycenter.testing.SafetyCenterTestConfigs.Companion.SOURCE_ID_2
import com.android.safetycenter.testing.SafetyCenterTestConfigs.Companion.SOURCE_ID_3
import com.android.safetycenter.testing.SafetyCenterTestConfigs.Companion.SOURCE_ID_4
import com.android.safetycenter.testing.SafetyCenterTestConfigs.Companion.SOURCE_ID_5
import com.android.safetycenter.testing.SafetyCenterTestHelper
import com.android.safetycenter.testing.SafetySourceIntentHandler.Request
import com.android.safetycenter.testing.SafetySourceIntentHandler.Response
import com.android.safetycenter.testing.SafetySourceReceiver
import com.android.safetycenter.testing.SafetySourceTestData
import com.android.safetycenter.testing.UiTestHelper.MORE_ISSUES_LABEL
import com.android.safetycenter.testing.UiTestHelper.clickMoreIssuesCard
import com.android.safetycenter.testing.UiTestHelper.resetRotation
import com.android.safetycenter.testing.UiTestHelper.rotate
import com.android.safetycenter.testing.UiTestHelper.waitAllTextDisplayed
import com.android.safetycenter.testing.UiTestHelper.waitAllTextNotDisplayed
import com.android.safetycenter.testing.UiTestHelper.waitButtonDisplayed
import com.android.safetycenter.testing.UiTestHelper.waitDisplayed
import com.android.safetycenter.testing.UiTestHelper.waitNotDisplayed
import com.android.safetycenter.testing.UiTestHelper.waitSourceIssueDisplayed
import com.android.safetycenter.testing.UiTestHelper.waitSourceIssueNotDisplayed
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Functional tests for generic subpages in Safety Center. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class SafetyCenterSubpagesTest {

    @get:Rule val disableAnimationRule = DisableAnimationRule()

    @get:Rule val freezeRotationRule = FreezeRotationRule()

    private val context: Context = getApplicationContext()
    private val safetyCenterTestHelper = SafetyCenterTestHelper(context)
    private val safetySourceTestData = SafetySourceTestData(context)
    private val safetyCenterTestConfigs = SafetyCenterTestConfigs(context)

    // JUnit's Assume is not supported in @BeforeClass by the tests runner, so this is used to
    // manually skip the setup and teardown methods.
    private val shouldRunTests = context.deviceSupportsSafetyCenter()

    @Before
    fun assumeDeviceSupportsSafetyCenterToRunTests() {
        assumeTrue(shouldRunTests)
    }

    @Before
    fun enableSafetyCenterBeforeTest() {
        if (!shouldRunTests) {
            return
        }
        safetyCenterTestHelper.setup()
        SafetyCenterFlags.showSubpages = true
    }

    @After
    fun clearDataAfterTest() {
        if (!shouldRunTests) {
            return
        }
        safetyCenterTestHelper.reset()
        UiAutomatorUtils2.getUiDevice().resetRotation()
    }

    @Test
    fun launchSafetyCenter_withSubpagesIntentExtra_showsSubpageTitle() {
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.multipleSourceGroupsConfig)
        val extras = Bundle()
        extras.putString(EXTRA_SAFETY_SOURCES_GROUP_ID, MULTIPLE_SOURCES_GROUP_ID_1)

        context.launchSafetyCenterActivity(extras) {
            // CollapsingToolbar title can't be found by text, so using description instead.
            waitDisplayed(
                By.desc(
                    context.getString(
                        safetyCenterTestConfigs.multipleSourceGroupsConfig.safetySourcesGroups
                            .first()!!
                            .titleResId
                    )
                )
            )
        }
    }

    @Test
    fun launchSafetyCenter_withSubpagesIntentExtraButFlagDisabled_showsHomepageTitle() {
        SafetyCenterFlags.showSubpages = false
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.multipleSourceGroupsConfig)
        val extras = Bundle()
        extras.putString(EXTRA_SAFETY_SOURCES_GROUP_ID, MULTIPLE_SOURCES_GROUP_ID_1)

        context.launchSafetyCenterActivity(extras) {
            // CollapsingToolbar title can't be found by text, so using description instead.
            waitDisplayed(By.desc("Security & privacy"))
        }
    }

    @Test
    fun launchSafetyCenter_withNonExistingGroupID_displaysNothing() {
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.multipleSourceGroupsConfig)
        val extras = Bundle()
        extras.putString(EXTRA_SAFETY_SOURCES_GROUP_ID, "non_existing_group_id")

        context.launchSafetyCenterActivity(extras) {
            waitNotDisplayed(
                By.desc(
                    context.getString(
                        safetyCenterTestConfigs.multipleSourceGroupsConfig.safetySourcesGroups
                            .first()!!
                            .titleResId
                    )
                )
            )
        }
    }

    @Test
    fun launchSafetyCenter_withMultipleGroups_showsHomepageEntries() {
        val sourceTestData = safetySourceTestData.information
        with(safetyCenterTestHelper) {
            setConfig(safetyCenterTestConfigs.multipleSourceGroupsConfig)
            setData(SOURCE_ID_1, sourceTestData)
            setData(SOURCE_ID_2, sourceTestData)
            setData(SOURCE_ID_3, sourceTestData)
            setData(SOURCE_ID_4, sourceTestData)
            setData(SOURCE_ID_5, sourceTestData)
        }
        val firstGroup =
            safetyCenterTestConfigs.multipleSourceGroupsConfig.safetySourcesGroups.first()
        val lastGroup =
            safetyCenterTestConfigs.multipleSourceGroupsConfig.safetySourcesGroups.last()

        context.launchSafetyCenterActivity {
            waitAllTextDisplayed(
                context.getString(firstGroup.titleResId),
                context.getString(firstGroup.summaryResId),
                context.getString(lastGroup.titleResId),
                context.getString(lastGroup.summaryResId)
            )

            openPageAndExit(context.getString(lastGroup.titleResId)) {
                // Verifying that the subpage is opened with collapsing toolbar title
                waitDisplayed(By.desc(context.getString(lastGroup.titleResId)))
                waitAllTextNotDisplayed(context.getString(lastGroup.summaryResId))
            }
        }
    }

    @Test
    fun launchSafetyCenter_withMultipleGroupsButFlagDisabled_showsExpandAndCollapseEntries() {
        SafetyCenterFlags.showSubpages = false
        val sourceTestData = safetySourceTestData.information
        with(safetyCenterTestHelper) {
            setConfig(safetyCenterTestConfigs.multipleSourceGroupsConfig)
            setData(SOURCE_ID_1, sourceTestData)
            setData(SOURCE_ID_2, sourceTestData)
            setData(SOURCE_ID_3, sourceTestData)
            setData(SOURCE_ID_4, sourceTestData)
            setData(SOURCE_ID_5, sourceTestData)
        }
        val firstGroup =
            safetyCenterTestConfigs.multipleSourceGroupsConfig.safetySourcesGroups.first()
        val lastGroup =
            safetyCenterTestConfigs.multipleSourceGroupsConfig.safetySourcesGroups.last()

        context.launchSafetyCenterActivity {
            waitAllTextDisplayed(
                context.getString(firstGroup.titleResId),
                context.getString(firstGroup.summaryResId),
                context.getString(lastGroup.titleResId)
            )
            waitDisplayed(By.text(context.getString(lastGroup.summaryResId))) { it.click() }

            // Verifying that the group is expanded and sources are displayed
            waitAllTextDisplayed(sourceTestData.status!!.title, sourceTestData.status!!.summary)
            waitAllTextNotDisplayed(context.getString(lastGroup.summaryResId))
        }
    }

    @Test
    fun launchSafetyCenter_redirectBackFromSubpage_showsHomepageEntries() {
        with(safetyCenterTestHelper) {
            setConfig(safetyCenterTestConfigs.multipleSourceGroupsConfig)
            setData(SOURCE_ID_1, safetySourceTestData.information)
            setData(SOURCE_ID_2, safetySourceTestData.information)
        }
        val firstGroup =
            safetyCenterTestConfigs.multipleSourceGroupsConfig.safetySourcesGroups.first()

        context.launchSafetyCenterActivity {
            // Verifying that both entry title and summary are displayed on homepage
            waitAllTextDisplayed(
                context.getString(firstGroup.titleResId),
                context.getString(firstGroup.summaryResId)
            )

            openPageAndExit(context.getString(firstGroup.titleResId)) {
                // Verifying that only collapsing toolbar title is displayed for subpage
                waitDisplayed(By.desc(context.getString(firstGroup.titleResId)))
                waitAllTextNotDisplayed(context.getString(firstGroup.summaryResId))
            }

            // Verifying that the homepage is opened again
            waitAllTextDisplayed(
                context.getString(firstGroup.titleResId),
                context.getString(firstGroup.summaryResId)
            )
        }
    }

    @Test
    fun entryListWithMultipleSources_clickingOnHomepageEntry_showsSubpageEntries() {
        with(safetyCenterTestHelper) {
            setConfig(safetyCenterTestConfigs.multipleSourcesConfig)
            setData(
                SOURCE_ID_1,
                safetySourceTestData.buildSafetySourceDataWithSummary(
                    severityLevel = SafetySourceData.SEVERITY_LEVEL_INFORMATION,
                    entryTitle = SAFETY_SOURCE_1_TITLE,
                    entrySummary = SAFETY_SOURCE_1_SUMMARY
                )
            )
            setData(
                SOURCE_ID_2,
                safetySourceTestData.buildSafetySourceDataWithSummary(
                    severityLevel = SafetySourceData.SEVERITY_LEVEL_INFORMATION,
                    entryTitle = SAFETY_SOURCE_2_TITLE,
                    entrySummary = SAFETY_SOURCE_2_SUMMARY
                )
            )
            setData(
                SOURCE_ID_3,
                safetySourceTestData.buildSafetySourceDataWithSummary(
                    severityLevel = SafetySourceData.SEVERITY_LEVEL_INFORMATION,
                    entryTitle = SAFETY_SOURCE_3_TITLE,
                    entrySummary = SAFETY_SOURCE_3_SUMMARY
                )
            )
        }
        val firstGroup = safetyCenterTestConfigs.multipleSourcesConfig.safetySourcesGroups[0]
        val secondGroup = safetyCenterTestConfigs.multipleSourcesConfig.safetySourcesGroups[1]

        context.launchSafetyCenterActivity {
            // Verifying that subpage entries of the first group are displayed
            openPageAndExit(context.getString(firstGroup.titleResId)) {
                waitAllTextNotDisplayed(context.getString(firstGroup.summaryResId))
                waitAllTextDisplayed(
                    SAFETY_SOURCE_1_TITLE,
                    SAFETY_SOURCE_1_SUMMARY,
                    SAFETY_SOURCE_2_TITLE,
                    SAFETY_SOURCE_2_SUMMARY
                )
            }

            // Verifying that subpage entries of the second group are displayed
            openPageAndExit(context.getString(secondGroup.titleResId)) {
                waitAllTextNotDisplayed(context.getString(secondGroup.summaryResId))
                waitAllTextDisplayed(SAFETY_SOURCE_3_TITLE, SAFETY_SOURCE_3_SUMMARY)
            }
        }
    }

    @Test
    fun entryListWithSingleSource_clickingOnSubpageEntry_redirectsToDifferentScreen() {
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.singleSourceConfig)
        val sourcesGroup = safetyCenterTestConfigs.singleSourceConfig.safetySourcesGroups.first()
        val source: SafetySource = sourcesGroup.safetySources.first()

        context.launchSafetyCenterActivity {
            openPageAndExit(context.getString(sourcesGroup.titleResId)) {
                waitDisplayed(By.text(context.getString(source.titleResId))) { it.click() }
                waitButtonDisplayed("Exit test activity") { it.click() }
                waitAllTextDisplayed(
                    context.getString(source.titleResId),
                    context.getString(source.summaryResId)
                )
            }
        }
    }

    @Test
    fun entryListWithSingleSource_clickingTheInfoIcon_redirectsToDifferentScreen() {
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.singleSourceConfig)
        val sourceTestData = safetySourceTestData.informationWithIconAction
        safetyCenterTestHelper.setData(SINGLE_SOURCE_ID, sourceTestData)
        val sourcesGroup = safetyCenterTestConfigs.singleSourceConfig.safetySourcesGroups.first()

        context.launchSafetyCenterActivity {
            openPageAndExit(context.getString(sourcesGroup.titleResId)) {
                waitDisplayed(By.desc("Information")) { it.click() }
                waitButtonDisplayed("Exit test activity") { it.click() }
                waitAllTextDisplayed(sourceTestData.status!!.title, sourceTestData.status!!.summary)
            }
        }
    }

    @Test
    fun entryListWithSingleSource_clickingTheGearIcon_redirectsToDifferentScreen() {
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.singleSourceConfig)
        val sourceTestData = safetySourceTestData.informationWithGearIconAction
        safetyCenterTestHelper.setData(SINGLE_SOURCE_ID, sourceTestData)
        val sourcesGroup = safetyCenterTestConfigs.singleSourceConfig.safetySourcesGroups.first()

        context.launchSafetyCenterActivity {
            openPageAndExit(context.getString(sourcesGroup.titleResId)) {
                waitDisplayed(By.desc("Settings")) { it.click() }
                waitButtonDisplayed("Exit test activity") { it.click() }
                waitAllTextDisplayed(sourceTestData.status!!.title, sourceTestData.status!!.summary)
            }
        }
    }

    @Test
    fun entryListWithSingleSource_clickingSourceWithNullPendingIntent_doesNothing() {
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.singleSourceInvalidIntentConfig)
        val sourceTestData = safetySourceTestData.informationWithNullIntent
        safetyCenterTestHelper.setData(SINGLE_SOURCE_ID, sourceTestData)
        val sourcesGroup =
            safetyCenterTestConfigs.singleSourceInvalidIntentConfig.safetySourcesGroups.first()

        context.launchSafetyCenterActivity {
            openPageAndExit(context.getString(sourcesGroup.titleResId)) {
                waitDisplayed(By.text(sourceTestData.status!!.title.toString())) { it.click() }

                // Verifying that clicking on the entry doesn't redirect to any other screen
                waitAllTextDisplayed(sourceTestData.status!!.title, sourceTestData.status!!.summary)
            }
        }
    }

    @Test
    fun entryListWithSingleSource_updateSafetySourceData_displayedDataIsUpdated() {
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.singleSourceConfig)
        val sourcesGroup = safetyCenterTestConfigs.singleSourceConfig.safetySourcesGroups.first()
        val source: SafetySource = sourcesGroup.safetySources.first()

        context.launchSafetyCenterActivity(withReceiverPermission = true) {
            openPageAndExit(context.getString(sourcesGroup.titleResId)) {
                waitAllTextDisplayed(
                    context.getString(source.titleResId),
                    context.getString(source.summaryResId)
                )
            }

            SafetySourceReceiver.setResponse(
                Request.Refresh(SINGLE_SOURCE_ID),
                Response.SetData(
                    safetySourceTestData.buildSafetySourceDataWithSummary(
                        severityLevel = SafetySourceData.SEVERITY_LEVEL_RECOMMENDATION,
                        entryTitle = "Updated title",
                        entrySummary = "Updated summary"
                    )
                )
            )

            openPageAndExit(context.getString(sourcesGroup.titleResId)) {
                waitAllTextNotDisplayed(
                    context.getString(source.titleResId),
                    context.getString(source.summaryResId)
                )
                waitAllTextDisplayed("Updated title", "Updated summary")
            }
        }
    }

    @Test
    fun entryListWithSingleSource_updateSafetySourceDataAndRotate_displayedDataIsNotUpdated() {
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.singleSourceConfig)
        val sourcesGroup = safetyCenterTestConfigs.singleSourceConfig.safetySourcesGroups.first()
        val source: SafetySource = sourcesGroup.safetySources.first()

        context.launchSafetyCenterActivity(withReceiverPermission = true) {
            openPageAndExit(context.getString(sourcesGroup.titleResId)) {
                waitAllTextDisplayed(
                    context.getString(source.titleResId),
                    context.getString(source.summaryResId)
                )

                SafetySourceReceiver.setResponse(
                    Request.Refresh(SINGLE_SOURCE_ID),
                    Response.SetData(
                        safetySourceTestData.buildSafetySourceDataWithSummary(
                            severityLevel = SafetySourceData.SEVERITY_LEVEL_RECOMMENDATION,
                            entryTitle = "Updated title",
                            entrySummary = "Updated summary"
                        )
                    )
                )
                UiAutomatorUtils2.getUiDevice().rotate()

                waitAllTextDisplayed(
                    context.getString(source.titleResId),
                    context.getString(source.summaryResId)
                )
                waitAllTextNotDisplayed("Updated title", "Updated summary")
            }
        }
    }

    @Test
    fun issueCard_withMultipleGroups_onlyRelevantSubpageHasIssueCard() {
        /* The default attribution title for an issue card is same as the entry group title on the
         * homepage. This causes test flakiness as UiAutomator is unable to choose from duplicate
         * strings. To address that, an issue with a different attribution title is used here. */
        val sourceData = safetySourceTestData.informationWithIssueWithAttributionTitle
        val issue = sourceData.issues[0]

        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.multipleSourcesConfig)
        val firstGroup = safetyCenterTestConfigs.multipleSourcesConfig.safetySourcesGroups[0]
        val secondGroup = safetyCenterTestConfigs.multipleSourcesConfig.safetySourcesGroups[1]
        safetyCenterTestHelper.setData(SOURCE_ID_3, sourceData)

        context.launchSafetyCenterActivity {
            // Verify that homepage has the issue card
            waitSourceIssueDisplayed(issue)

            // Verify that irrelevant subpage doesn't have the issue card
            openPageAndExit(context.getString(firstGroup.titleResId)) {
                waitSourceIssueNotDisplayed(issue)
            }
            // Verify that relevant subpage has the issue card
            openPageAndExit(context.getString(secondGroup.titleResId)) {
                waitSourceIssueDisplayed(issue)
            }
        }
    }

    @Test
    fun issueCard_updateSafetySourceData_subpageDisplaysUpdatedIssue() {
        val initialDataToDisplay = safetySourceTestData.informationWithIssueWithAttributionTitle
        val updatedDataToDisplay = safetySourceTestData.criticalWithIssueWithAttributionTitle

        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.singleSourceConfig)
        val sourcesGroup = safetyCenterTestConfigs.singleSourceConfig.safetySourcesGroups.first()
        safetyCenterTestHelper.setData(SINGLE_SOURCE_ID, initialDataToDisplay)

        context.launchSafetyCenterActivity {
            openPageAndExit(context.getString(sourcesGroup.titleResId)) {
                waitSourceIssueDisplayed(initialDataToDisplay.issues[0])

                safetyCenterTestHelper.setData(SINGLE_SOURCE_ID, updatedDataToDisplay)

                waitSourceIssueDisplayed(updatedDataToDisplay.issues[0])
            }
        }
    }

    @Test
    fun issueCard_resolveIssueOnSubpage_issueDismisses() {
        val sourceData = safetySourceTestData.criticalWithIssueWithAttributionTitle
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.singleSourceConfig)
        safetyCenterTestHelper.setData(SINGLE_SOURCE_ID, sourceData)
        val sourcesGroup = safetyCenterTestConfigs.singleSourceConfig.safetySourcesGroups.first()
        val issue = sourceData.issues[0]
        val action = issue.actions[0]

        // Clear the data when action is triggered to simulate resolution.
        SafetySourceReceiver.setResponse(
            Request.ResolveAction(SINGLE_SOURCE_ID),
            Response.ClearData
        )

        context.launchSafetyCenterActivity(withReceiverPermission = true) {
            openPageAndExit(context.getString(sourcesGroup.titleResId)) {
                waitSourceIssueDisplayed(issue)
                waitButtonDisplayed(action.label) { it.click() }

                // Wait for success message to go away, verify issue no longer displayed
                waitAllTextNotDisplayed(action.successMessage)
                waitSourceIssueNotDisplayed(issue)
            }
        }
    }

    @Test
    fun issueCard_confirmDismissalOnSubpage_dismissesIssue() {
        val sourceData = safetySourceTestData.criticalWithIssueWithAttributionTitle
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.singleSourceConfig)
        safetyCenterTestHelper.setData(SINGLE_SOURCE_ID, sourceData)
        val sourcesGroup = safetyCenterTestConfigs.singleSourceConfig.safetySourcesGroups.first()
        val issue = sourceData.issues[0]

        context.launchSafetyCenterActivity {
            openPageAndExit(context.getString(sourcesGroup.titleResId)) {
                waitSourceIssueDisplayed(issue)
                waitDisplayed(By.desc("Dismiss")) { it.click() }

                waitAllTextDisplayed("Dismiss this alert?")
                waitButtonDisplayed("Dismiss") { it.click() }

                waitSourceIssueNotDisplayed(issue)
            }
        }
    }

    @Test
    fun issueCard_dismissOnSubpageWithRotation_cancellationPersistsIssue() {
        val sourceData = safetySourceTestData.criticalWithIssueWithAttributionTitle
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.singleSourceConfig)
        safetyCenterTestHelper.setData(SINGLE_SOURCE_ID, sourceData)
        val sourcesGroup = safetyCenterTestConfigs.singleSourceConfig.safetySourcesGroups.first()
        val issue = sourceData.issues[0]

        context.launchSafetyCenterActivity {
            openPageAndExit(context.getString(sourcesGroup.titleResId)) {
                waitSourceIssueDisplayed(issue)
                waitDisplayed(By.desc("Dismiss")) { it.click() }
                waitAllTextDisplayed("Dismiss this alert?")

                UiAutomatorUtils2.getUiDevice().rotate()

                waitAllTextDisplayed("Dismiss this alert?")
                waitButtonDisplayed("Cancel") { it.click() }
                waitSourceIssueDisplayed(issue)
            }
        }
    }

    @Test
    fun moreIssuesCard_expandOnSubpage_showsAdditionalCard() {
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.multipleSourcesConfig)
        val sourcesGroup = safetyCenterTestConfigs.multipleSourcesConfig.safetySourcesGroups.first()
        val firstSourceData = safetySourceTestData.criticalWithIssueWithAttributionTitle
        val secondSourceData = safetySourceTestData.informationWithIssueWithAttributionTitle
        safetyCenterTestHelper.setData(SOURCE_ID_1, firstSourceData)
        safetyCenterTestHelper.setData(SOURCE_ID_2, secondSourceData)

        context.launchSafetyCenterActivity {
            openPageAndExit(context.getString(sourcesGroup.titleResId)) {
                waitSourceIssueDisplayed(firstSourceData.issues[0])
                waitAllTextDisplayed(MORE_ISSUES_LABEL)
                waitSourceIssueNotDisplayed(secondSourceData.issues[0])

                clickMoreIssuesCard()

                waitSourceIssueDisplayed(firstSourceData.issues[0])
                waitAllTextDisplayed(MORE_ISSUES_LABEL)
                waitSourceIssueDisplayed(secondSourceData.issues[0])
            }
        }
    }

    @Test
    fun brandChip_openSubpageFromHomepage_homepageReopensOnClick() {
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.singleSourceConfig)
        safetyCenterTestHelper.setData(SINGLE_SOURCE_ID, safetySourceTestData.information)
        val sourcesGroup = safetyCenterTestConfigs.singleSourceConfig.safetySourcesGroups.first()

        context.launchSafetyCenterActivity {
            // Verify that the homepage is opened
            waitAllTextDisplayed(
                context.getString(sourcesGroup.titleResId),
                context.getString(sourcesGroup.summaryResId)
            )

            // Open subpage by clicking on the group title
            waitDisplayed(By.text(context.getString(sourcesGroup.titleResId))) { it.click() }
            waitDisplayed(By.desc(context.getString(sourcesGroup.titleResId)))
            waitAllTextNotDisplayed(context.getString(sourcesGroup.summaryResId))

            // Open homepage again by clicking on the brand chip
            waitButtonDisplayed("Security & privacy") { it.click() }
            waitAllTextDisplayed(
                context.getString(sourcesGroup.titleResId),
                context.getString(sourcesGroup.summaryResId)
            )
        }
    }

    @Test
    fun brandChip_openSubpageFromIntent_homepageOpensOnClick() {
        safetyCenterTestHelper.setConfig(safetyCenterTestConfigs.singleSourceConfig)
        safetyCenterTestHelper.setData(SINGLE_SOURCE_ID, safetySourceTestData.information)
        val sourcesGroup = safetyCenterTestConfigs.singleSourceConfig.safetySourcesGroups.first()
        val extras = Bundle()
        extras.putString(EXTRA_SAFETY_SOURCES_GROUP_ID, sourcesGroup.id)

        context.launchSafetyCenterActivity(extras) {
            // Verify that the subpage is opened
            waitDisplayed(By.desc(context.getString(sourcesGroup.titleResId)))
            waitAllTextNotDisplayed(context.getString(sourcesGroup.summaryResId))

            // Open homepage by clicking on the brand chip
            waitButtonDisplayed("Security & privacy") { it.click() }
            waitAllTextDisplayed(
                context.getString(sourcesGroup.titleResId),
                context.getString(sourcesGroup.summaryResId)
            )
        }
    }

    @Test
    fun privacySubpage_openWithIntentExtra_showsSubpageData() {
        val config = safetyCenterTestConfigs.privacySubpageConfig
        safetyCenterTestHelper.setConfig(config)
        val sourcesGroup = config.safetySourcesGroups.first()
        val firstSource: SafetySource = sourcesGroup.safetySources.first()
        val lastSource: SafetySource = sourcesGroup.safetySources.last()
        val extras = Bundle()
        extras.putString(EXTRA_SAFETY_SOURCES_GROUP_ID, sourcesGroup.id)

        context.launchSafetyCenterActivity(extras) {
            waitAllTextDisplayed(
                context.getString(firstSource.titleResId),
                context.getString(firstSource.summaryResId),
                "Controls",
                "Data",
                context.getString(lastSource.titleResId),
                context.getString(lastSource.summaryResId)
            )
        }
    }

    @Test
    fun privacySubpage_clickingOnEntry_redirectsToDifferentScreen() {
        val config = safetyCenterTestConfigs.privacySubpageConfig
        safetyCenterTestHelper.setConfig(config)
        val sourcesGroup = config.safetySourcesGroups.first()
        val source: SafetySource = sourcesGroup.safetySources.first()
        val extras = Bundle()
        extras.putString(EXTRA_SAFETY_SOURCES_GROUP_ID, sourcesGroup.id)

        context.launchSafetyCenterActivity(extras) {
            waitDisplayed(By.text(context.getString(source.titleResId))) { it.click() }
            waitButtonDisplayed("Exit test activity") { it.click() }
            waitAllTextDisplayed(
                context.getString(source.titleResId),
                context.getString(source.summaryResId)
            )
        }
    }

    @Test
    fun privacySubpage_withMultipleIssues_displaysExpectedWarningCards() {
        val config = safetyCenterTestConfigs.privacySubpageConfig
        safetyCenterTestHelper.setConfig(config)
        val firstSourceData = safetySourceTestData.criticalWithIssueWithAttributionTitle
        val secondSourceData = safetySourceTestData.informationWithIssueWithAttributionTitle
        safetyCenterTestHelper.setData(PRIVACY_SOURCE_ID_1, firstSourceData)
        safetyCenterTestHelper.setData(PRIVACY_SOURCE_ID_2, secondSourceData)
        val extras = Bundle()
        extras.putString(EXTRA_SAFETY_SOURCES_GROUP_ID, config.safetySourcesGroups.first().id)

        context.launchSafetyCenterActivity(extras) {
            waitSourceIssueDisplayed(firstSourceData.issues[0])
            waitAllTextDisplayed(MORE_ISSUES_LABEL)
            waitSourceIssueNotDisplayed(secondSourceData.issues[0])

            clickMoreIssuesCard()

            waitSourceIssueDisplayed(firstSourceData.issues[0])
            waitAllTextDisplayed(MORE_ISSUES_LABEL)
            waitSourceIssueDisplayed(secondSourceData.issues[0])
        }
    }

    @Test
    fun privacySubpage_openWithIntentExtra_showsPrivacyControls() {
        val config = safetyCenterTestConfigs.privacySubpageConfig
        safetyCenterTestHelper.setConfig(config)
        val extras = Bundle()
        extras.putString(EXTRA_SAFETY_SOURCES_GROUP_ID, config.safetySourcesGroups.first().id)

        context.launchSafetyCenterActivity(extras) {
            waitAllTextDisplayed(
                "Camera access",
                "Microphone access",
                "Show clipboard access",
                "Show passwords",
                "Location Settings"
            )
        }
    }

    @Test
    fun privacySubpage_clickingOnLocationEntry_redirectsToLocationScreen() {
        val config = safetyCenterTestConfigs.privacySubpageConfig
        safetyCenterTestHelper.setConfig(config)
        val sourcesGroup = config.safetySourcesGroups.first()
        val source: SafetySource = sourcesGroup.safetySources.first()
        val extras = Bundle()
        extras.putString(EXTRA_SAFETY_SOURCES_GROUP_ID, sourcesGroup.id)

        context.launchSafetyCenterActivity(extras) {
            openPageAndExit("Location Settings") {
                waitDisplayed(By.desc("Location"))
                waitAllTextDisplayed("Use location")
            }

            waitAllTextDisplayed(
                context.getString(source.titleResId),
                context.getString(source.summaryResId)
            )
        }
    }

    private fun openPageAndExit(entryPoint: String, block: () -> Unit) {
        val uiDevice = UiAutomatorUtils2.getUiDevice()
        uiDevice.waitForIdle()

        // Opens page by clicking on the entry point
        waitDisplayed(By.text(entryPoint)) { it.click() }
        uiDevice.waitForIdle()

        // Executes the required verifications
        block()
        uiDevice.waitForIdle()

        // Exits page by pressing the back button
        uiDevice.pressBack()
        uiDevice.waitForIdle()
    }

    companion object {
        private const val SAFETY_SOURCE_1_TITLE = "Safety Source 1 Title"
        private const val SAFETY_SOURCE_1_SUMMARY = "Safety Source 1 Summary"
        private const val SAFETY_SOURCE_2_TITLE = "Safety Source 2 Title"
        private const val SAFETY_SOURCE_2_SUMMARY = "Safety Source 2 Summary"
        private const val SAFETY_SOURCE_3_TITLE = "Safety Source 3 Title"
        private const val SAFETY_SOURCE_3_SUMMARY = "Safety Source 3 Summary"
    }
}
