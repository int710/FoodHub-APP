import fs from "node:fs";
import path from "node:path";

const packageRoot = process.argv[2];
if (!packageRoot) throw new Error("Usage: node scripts/generate-tabler-icons.mjs <@tabler/icons package root>");

const mapping = {
  AccessTime: "clock",
  AccountBalanceWallet: "wallet",
  Add: "plus",
  Adjustments: "adjustments-horizontal",
  AlertTriangle: "alert-triangle",
  Armchair: "armchair",
  ArrowBack: "arrow-left",
  ArrowRight: "arrow-right",
  Bell: "bell",
  Call: "phone",
  CalendarToday: "calendar",
  CategoryCoffee: "coffee",
  CategoryDessert: "cake",
  CategoryDrink: "glass-full",
  CategoryMeat: "meat",
  CategorySoup: "bowl-spoon",
  CategoryVegetarian: "leaf",
  Cash: "cash",
  ChatBubble: "message-circle",
  Check: "check",
  CheckCircle: "circle-check",
  ChefHat: "chef-hat",
  ChevronDown: "chevron-down",
  ChevronRight: "chevron-right",
  ChevronUp: "chevron-up",
  Close: "x",
  CloudOff: "cloud-off",
  CreditCard: "credit-card",
  Dashboard: "layout-dashboard",
  Delete: "trash",
  Delivery: "truck-delivery",
  DoneAll: "checks",
  Edit: "edit",
  Flame: "flame",
  Flash: "bolt",
  GridView: "layout-grid",
  Headset: "headset",
  Home: "home",
  Info: "info-circle",
  MoreVertical: "dots-vertical",
  Payment: "cash",
  Photo: "photo",
  Power: "power",
  QrCode: "qrcode",
  QrScan: "scan",
  QrScanPosition: "scan-position",
  Receipt: "receipt",
  Refresh: "refresh",
  Reload: "reload",
  Restaurant: "tools-kitchen-2",
  Search: "search",
  Send: "send-2",
  Share: "share",
  ShieldCheck: "shield-check",
  ShoppingBag: "shopping-bag",
  ShoppingCart: "shopping-cart",
  Star: "star",
  Table: "armchair-2",
  Takeaway: "paper-bag",
  TrendingUp: "trending-up",
  User: "user",
  Wallet: "wallet",
};

const outputPath = path.resolve("app/src/main/java/com/example/foodhubapp/core/ui/icons/TablerIcons.kt");
const sourceRoot = path.resolve("app/src/main/java");
const kotlinFiles = (directory) => fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
  const target = path.join(directory, entry.name);
  return entry.isDirectory() ? kotlinFiles(target) : target.endsWith(".kt") && target !== outputPath ? [target] : [];
});
const usedProperties = new Set(
  kotlinFiles(sourceRoot).flatMap((file) =>
    [...fs.readFileSync(file, "utf8").matchAll(/TablerIcons\.([A-Za-z0-9_]+)/g)].map((match) => match[1]),
  ),
);

const nodes = JSON.parse(fs.readFileSync(path.join(packageRoot, "tabler-nodes-outline.json"), "utf8"));
const definitions = Object.entries(mapping).filter(([property]) => usedProperties.has(property)).map(([property, iconName]) => {
  const iconNodes = nodes[iconName];
  if (!iconNodes) throw new Error(`Missing Tabler icon: ${iconName}`);
  const unsupported = iconNodes.filter(([tag]) => tag !== "path");
  if (unsupported.length) throw new Error(`${iconName} contains unsupported SVG nodes: ${unsupported.map(([tag]) => tag).join(", ")}`);
  const paths = iconNodes.map(([, attributes]) => `            "${attributes.d}",`).join("\n");
  return `    val ${property}: ImageVector by lazy {\n        tablerIcon(\n            name = "Tabler${property}",\n            paths = listOf(\n${paths}\n            ),\n        )\n    }`;
}).join("\n\n");

const output = `// Generated from @tabler/icons 3.46.0 by scripts/generate-tabler-icons.mjs.
// Tabler Icons are MIT licensed: docs/licenses/TABLER_ICONS_LICENSE.txt
package com.example.foodhubapp.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object TablerIcons {
${definitions}
}

private fun tablerIcon(name: String, paths: List<String>): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { pathData ->
            addPath(
                pathData = PathParser().parsePathString(pathData).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
`;

fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, output, "utf8");

const licenseDir = path.resolve("docs/licenses");
fs.mkdirSync(licenseDir, { recursive: true });
fs.copyFileSync(path.join(packageRoot, "LICENSE"), path.join(licenseDir, "TABLER_ICONS_LICENSE.txt"));
