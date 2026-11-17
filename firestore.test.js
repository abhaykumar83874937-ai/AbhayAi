const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-smartstu-a8244afb-4733-42c7-8faa-ae12fccb641c";
const ALICE_UID = "alice_student_1";
const BOB_UID = "bob_student_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function aliceProfilePayload(uid = ALICE_UID) {
  return {
    userId: uid,
    displayName: "Alice",
    dailyGoalMinutes: 45,
    todayMinutesStudied: 15,
    todayQuestionsSolved: 2,
    streakDays: 3,
    preferredLanguage: "English",
    lastStudyDate: "2026-11-14",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function questionPayload(uid = ALICE_UID) {
  return {
    userId: uid,
    subject: "Mathematics",
    topic: "Quadratic Equations",
    questionText: "Solve x^2 - 5x + 6 = 0",
    answerSummary: "Step 1: Factor into (x-2)(x-3)=0. Roots: x=2, x=3.",
    mode: "math",
    language: "English",
    hasAttachment: false,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function planPayload(uid = ALICE_UID) {
  return {
    userId: uid,
    subject: "Physics",
    title: "Revise Newton's Laws",
    scheduledDate: "Today 5:00 PM",
    durationMinutes: 30,
    completed: false,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function notePayload(uid = ALICE_UID) {
  return {
    userId: uid,
    subject: "Biology",
    toolType: "notes",
    title: "Cell Organelles",
    content: "Mitochondria is the powerhouse of the cell.",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("questions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("studyPlans").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("savedNotes").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(aliceProfilePayload()));
});

test("Authenticated user can create, read, and update own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(aliceProfilePayload()));
  await assertSucceeds(profileRef.get());
  await assertSucceeds(
    profileRef.update({
      todayMinutesStudied: 30,
      todayQuestionsSolved: 3,
      updatedAt: new Date(),
    })
  );
});

test("Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(aliceProfilePayload()));
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user read and write are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(aliceProfilePayload()));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("questions").doc("q1").set(questionPayload()));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("questions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("questions").doc("q1").delete());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("studyPlans").doc("p1").set(planPayload(BOB_UID)));
});

test("Authenticated user can manage own questions, studyPlans, and savedNotes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(userDoc.collection("questions").doc("q1").set(questionPayload()));
  await assertSucceeds(userDoc.collection("questions").get());
  await assertSucceeds(userDoc.collection("questions").doc("q1").delete());

  await assertSucceeds(userDoc.collection("studyPlans").doc("p1").set(planPayload()));
  await assertSucceeds(userDoc.collection("studyPlans").doc("p1").update({ completed: true, updatedAt: new Date() }));
  await assertSucceeds(userDoc.collection("studyPlans").get());

  await assertSucceeds(userDoc.collection("savedNotes").doc("n1").set(notePayload()));
  await assertSucceeds(userDoc.collection("savedNotes").get());
});

test("Invalid enum or oversized strings are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  await assertFails(
    userDoc.collection("questions").doc("q_bad").set({
      ...questionPayload(),
      mode: "invalid_mode",
    })
  );
  await assertFails(
    userDoc.collection("savedNotes").doc("n_bad").set({
      ...notePayload(),
      toolType: "hack",
    })
  );
  await assertFails(
    userDoc.collection("studyPlans").doc("p_bad").set({
      ...planPayload(),
      durationMinutes: 9999,
    })
  );
});
