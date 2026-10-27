import { useEffect, useState } from "react";
import { fetchProducts, type Product } from "./api";
import { ProductCard } from "./ProductCard";

type ProductListLoadingState = {
  status: "loading";
};

type ProductListFailedState = {
  status: "error";
};

type ProductListSuccessState = {
  status: "success";
  products: Product[];
};

type ProductListState =
  ProductListLoadingState | ProductListFailedState | ProductListSuccessState;

export function ProductList() {
  const [listState, setListState] = useState<ProductListState>({
    status: "loading",
  });

  useEffect(() => {
    const controller = new AbortController();

    fetchProducts(controller.signal)
      .then((products) =>
        setListState({
          status: "success",
          products,
        }),
      )
      .catch((err) => {
        if (controller.signal.aborted) {
          return;
        }

        console.error(err);

        setListState({
          status: "error",
        });
      });

    return () => controller.abort();
  }, []);

  if (listState.status === "loading") {
    return <p role="status">Loading products...</p>;
  } else if (listState.status === "error") {
    return <p role="alert">Couldn't load products. Please try again later.</p>;
  } else if (listState.products.length === 0) {
    return <p>No products available.</p>;
  }

  return (
    <ul>
      {listState.products.map((product) => (
        <li key={product.id}>
          <ProductCard product={product} />
        </li>
      ))}
    </ul>
  );
}
