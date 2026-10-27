import { exit, stderr } from "node:process";

function toSqlString(str) {
  return "'" + str.replaceAll("'", "''") + "'";
}

const response = await fetch("https://dummyjson.com/products?limit=50");

if (!response.ok) {
  stderr.write(
    `Failed to fetch sample products (status=${response.status}). Abort. \n`,
  );
  exit(1);
}

const json = await response.json();
const existingProductNames = [
  "Essence Mascara Lash Princess",
  "Powder Canister",
  "Red Lipstick",
];
const products = json.products.filter(
  (p) => !existingProductNames.includes(p.title),
);

console.log("INSERT INTO product (id, name, price, description, image_url)");
console.log("VALUES");

const valueRows = [];
for (const product of products) {
  const values = [
    "nextval('product_seq')",
    toSqlString(product.title),
    product.price,
    toSqlString(product.description),
    toSqlString(product.thumbnail),
  ];

  valueRows.push("    (" + values.join(", ") + ")");
}

console.log(valueRows.join(",\n") + ";");
