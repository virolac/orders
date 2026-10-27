import { useEffect, useState } from "react";

type Product = {
  id: number;
  name: string;
  price: number;
};

async function fetchProducts(): Promise<Product[]> {
  const response = await fetch("/api/products");
  const products = await response.json();

  return products;
}

function App() {
  const [products, setProducts] = useState<Product[]>([]);

  useEffect(() => {
    fetchProducts().then(setProducts);
  }, []);

  return (
    <ul>
      {products.map((product) => (
        <li key={product.id}>
          {product.name} ({product.price})
        </li>
      ))}
    </ul>
  );
}

export default App;
