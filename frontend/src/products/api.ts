export type Product = {
  id: number;
  name: string;
  price: number;
  description: string | null;
  imageUrl: string | null;
};

export async function fetchProducts(signal: AbortSignal): Promise<Product[]> {
  const response = await fetch("/api/products", { signal });
  if (!response.ok) {
    throw new Error(`Failed to fetch products (status=${response.status})`);
  }

  return await response.json();
}
