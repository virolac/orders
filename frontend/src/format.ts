const priceFormatter = new Intl.NumberFormat(undefined, {
  style: "currency",
  currency: "EUR",
});

export function formatPrice(price: number): string {
  return priceFormatter.format(price);
}

const dateFormatter = new Intl.DateTimeFormat(undefined, {
  dateStyle: "medium",
  timeStyle: "short",
});

export function formatDate(isoString: string): string {
  return dateFormatter.format(new Date(isoString));
}
