// Chụp đúng khung điện thoại của một màn trong mockup HTML, theo mã SCR.
//
//   node tools/ui-check/mockup-shot.mjs --scr SCR-0301 --out /tmp/ui/SCR-0301.mockup.png
//   node tools/ui-check/mockup-shot.mjs --list
//
// Ảnh ra rộng đúng 1080px (khung mockup 390 CSS px × scale 1080/390) để đặt cạnh ảnh `adb screencap`
// của emulator 1080×2400 mà không phải co giãn. Chạy bằng Chrome đã cài trên máy (channel "chrome"),
// không tải Chromium riêng.

import { chromium } from "playwright-core";
import { existsSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";

const DEFAULT_HTML =
  "/Users/temp/DATA/AREA/AI_Knowledge/claude_station/data/knowledge/mvkzxzyw-0628c24d-e91/take-home/flowdrama-android-mockup.html";
const PHONE_CSS_WIDTH = 390;
const TARGET_WIDTH = 1080;

function parseArgs(argv) {
  const args = { html: process.env.MOCKUP_HTML ?? DEFAULT_HTML, scr: null, out: null, list: false };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--list") args.list = true;
    else if (a === "--html") args.html = argv[++i];
    else if (a === "--scr") args.scr = argv[++i];
    else if (a === "--out") args.out = argv[++i];
  }
  return args;
}

const args = parseArgs(process.argv.slice(2));
if (!existsSync(args.html)) {
  console.error(`Không thấy mockup: ${args.html} (đặt --html hoặc MOCKUP_HTML)`);
  process.exit(2);
}
if (!args.list && (!args.scr || !args.out)) {
  console.error("Cần --scr SCR-xxxx và --out <file.png> (hoặc --list)");
  process.exit(2);
}

const browser = await chromium.launch({ channel: "chrome" });
try {
  const page = await browser.newPage({
    viewport: { width: 1400, height: 1000 },
    deviceScaleFactor: TARGET_WIDTH / PHONE_CSS_WIDTH,
  });
  await page.goto(`file://${resolve(args.html)}`, { waitUntil: "load" });

  // Mỗi màn là một `.unit` gồm `.phone` + caption `.cap .n` có mã SCR.
  const units = await page.$$eval(".unit", (nodes) =>
    nodes.map((n, index) => ({
      index,
      caption: (n.querySelector(".cap .n")?.textContent ?? "").replace(/\s+/g, " ").trim(),
    })),
  );

  if (args.list) {
    for (const u of units) console.log(u.caption);
    process.exit(0);
  }

  const match = units.find((u) => new RegExp(`\\b${args.scr}\\b`).test(u.caption));
  if (!match) {
    console.error(`Không có màn ${args.scr} trong mockup. Có: ${units.map((u) => u.caption).join(" | ")}`);
    process.exit(3);
  }

  const phone = page.locator(".unit").nth(match.index).locator(".phone");
  mkdirSync(dirname(resolve(args.out)), { recursive: true });
  await phone.screenshot({ path: args.out, animations: "disabled" });
  console.log(`${args.scr} → ${args.out}  (${match.caption})`);
} finally {
  await browser.close();
}
