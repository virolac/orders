const priceFormatter = new Intl.NumberFormat(undefined, {
  style: "currency",
  currency: "EUR",
});

export function formatPrice(price: number): string {
  return priceFormatter.format(price);
}
