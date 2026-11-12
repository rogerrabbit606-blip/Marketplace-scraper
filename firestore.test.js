const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validScoutPayload(uid, overrides = {}) {
  const now = new Date();
  return {
    userId: uid,
    title: "Free Solid Wood Dressers",
    keywords: "solid wood dresser",
    category: "Furniture",
    dealMode: "FREE_ONLY",
    maxPrice: 0,
    citySlug: "sfbay",
    cityName: "San Francisco Bay",
    radiusMiles: 25,
    targetResale: 180,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validFlipPayload(uid, overrides = {}) {
  const now = new Date();
  return {
    userId: uid,
    title: "Mid-Century Walnut Dresser",
    sourcePlatform: "Facebook Marketplace",
    category: "Furniture",
    acquisitionType: "FREE",
    buyPriceCents: 0,
    repairCostCents: 1500,
    estResaleCents: 22000,
    soldPriceCents: 0,
    status: "SCOUTING",
    listingUrl: "https://www.facebook.com/marketplace",
    notes: "Solid dovetail drawers",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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

test("Unauthenticated user: cannot read or write scouts or flips", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("scouts").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("flips").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("scouts")
      .doc("scout_1")
      .set(validScoutPayload(ALICE_UID))
  );
});

test("Owner: can create, list, update, and delete own scout", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const scoutRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("scouts")
    .doc("scout_1");

  await assertSucceeds(scoutRef.set(validScoutPayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scouts").get()
  );
  await assertSucceeds(
    scoutRef.update({
      dealMode: "CASH_BARGAIN",
      maxPrice: 60,
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(scoutRef.delete());
});

test("Cross-user isolation: Bob cannot read or write Alice's scouts or flips", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("scouts")
      .doc("scout_1")
      .set(validScoutPayload(ALICE_UID));
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("flips")
      .doc("flip_1")
      .set(validFlipPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("scouts").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flips")
      .doc("flip_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("flips")
      .doc("flip_1")
      .delete()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("scouts")
      .doc("scout_2")
      .set(validScoutPayload(BOB_UID))
  );
});

test("Schema & invariant validation: rejects ghost fields, invalid free price, and bad IDs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const scoutsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("scouts");
  const flipsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("flips");

  // Ghost field rejected
  await assertFails(
    scoutsCol
      .doc("scout_ghost")
      .set(validScoutPayload(ALICE_UID, { isAdmin: true }))
  );

  // FREE_ONLY with non-zero maxPrice rejected
  await assertFails(
    scoutsCol
      .doc("scout_bad_free")
      .set(validScoutPayload(ALICE_UID, { dealMode: "FREE_ONLY", maxPrice: 25 }))
  );

  // FREE flip with non-zero buyPriceCents rejected
  await assertFails(
    flipsCol
      .doc("flip_bad_free")
      .set(
        validFlipPayload(ALICE_UID, {
          acquisitionType: "FREE",
          buyPriceCents: 1000,
        })
      )
  );

  // Invalid doc ID rejected
  await assertFails(
    flipsCol.doc("bad id with spaces").set(validFlipPayload(ALICE_UID))
  );
});

test("Terminal state lock: SOLD flip item cannot be updated after marking SOLD", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const flipRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("flips")
    .doc("flip_sold_1");

  await assertSucceeds(flipRef.set(validFlipPayload(ALICE_UID)));
  await assertSucceeds(
    flipRef.update({
      status: "SOLD",
      soldPriceCents: 21000,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    flipRef.update({
      notes: "Trying to alter sold record",
      updatedAt: new Date(),
    })
  );
});
