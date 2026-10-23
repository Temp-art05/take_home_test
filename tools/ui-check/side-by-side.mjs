// Ghép ảnh mockup và ảnh chụp app thành MỘT ảnh, hai cột cùng chiều rộng, để chấm bằng mắt một lần.
//
//   node tools/ui-check/side-by-side.mjs --mockup a.png --app b.png --out compare.png [--title "SCR-0301"]
//
// Hai ảnh được scale về cùng chiều rộng 540px rồi canh theo mép trên — status bar của cả hai thẳng
// hàng, nên lệch dọc của từng khối đọc được bằng cách dóng ngang.

import { chromium } from "playwright-core";
import { readFileSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";

const args = {};
const argv = process.argv.slice(2);
for (let i = 0; i < argv.length; i += 2) args[argv[i].replace(/^--/, "")] = argv[i + 1];
if (!args.mockup || !args.app || !args.out) {
  console.error("Cần --mockup <png> --app <png> --out <png>");
  process.exit(2);
}

const dataUri = (p) => `data:image/png;base64,${readFileSync(p).toString("base64")}`;
const title = args.title ?? "";

const html = `<!doctype html><html><body style="margin:0;background:#050506;font:600 22px system-ui;color:#ddd">
<div id="sheet" style="display:inline-flex;gap:24px;padding:24px;align-items:flex-start;background:#050506">
  <figure style="margin:0;width:540px"><figcaption style="margin-bottom:12px">MOCKUP ${title}</figcaption>
    <img src="${dataUri(args.mockup)}" style="width:540px;display:block"></figure>
  <figure style="margin:0;width:540px"><figcaption style="margin-bottom:12px">APP ${title}</figcaption>
    <img src="${dataUri(args.app)}" style="width:540px;display:block"></figure>
</div></body></html>`;

const browser = await chromium.launch({ channel: "chrome" });
try {
  const page = await browser.newPage({ viewport: { width: 1152, height: 800 } });
  await page.setContent(html, { waitUntil: "load" });
  mkdirSync(dirname(resolve(args.out)), { recursive: true });
  // Chụp đúng khối nội dung: `fullPage` của Chrome ghép nhiều lát và lặp đoạn khi trang dài.
  await page.locator("#sheet").screenshot({ path: args.out });
  console.log(`→ ${args.out}`);
} finally {
  await browser.close();
}
