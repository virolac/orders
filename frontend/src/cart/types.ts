import type { Product } from "../products/api";

export type CartItem = {
  product: Product;
  quantity: number;
};