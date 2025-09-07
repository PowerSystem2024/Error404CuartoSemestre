// cart.js

// PUBLIC KEY
const MP_PUBLIC_KEY = "APP_USR-c7560963-30d3-468c-bb6d-55da34b40f9d";

// Elementos del modal / UI
const modalContainer = document.getElementById("modal-container");
const modalOverlay = document.getElementById("modal-overlay");
const cartBtn = document.getElementById("cart-btn");
const cartCounter = document.getElementById("cart-counter");

// Instancia del SDK de Mercado Pago (frontend)
let mercadopago;
(function ensureMpLoaded() {
  if (!window.MercadoPago) {
    console.error("SDK de Mercado Pago no encontrado. Asegúrate de incluir <script src=\"https://sdk.mercadopago.com/js/v2\"></script> antes de cart.js");
    return;
  }
  mercadopago = new MercadoPago(MP_PUBLIC_KEY, { locale: "es-AR" });
})();

const displayCart = () => {
  modalContainer.innerHTML = "";
  modalContainer.style.display = "block";
  modalOverlay.style.display = "block";

  // Header
  const modalHeader = document.createElement("div");

  const modalClose = document.createElement("div");
  modalClose.innerText = "❌";
  modalClose.className = "modal-close";
  modalHeader.append(modalClose);

  modalClose.addEventListener("click", () => {
    modalContainer.style.display = "none";
    modalOverlay.style.display = "none";
  });

  const modalTitle = document.createElement("div");
  modalTitle.innerText = "Carrito de compras";
  modalTitle.className = "modal-title";
  modalHeader.append(modalTitle);

  modalContainer.appendChild(modalHeader);

  // Body
  if (cart.length > 0) {
    cart.forEach((product) => {
      const modalBody = document.createElement("div");
      modalBody.className = "modal-body";
      modalBody.innerHTML = `
        <div class="product">
          <img class="product-img" src="${product.img}"/>
          <div class="product-info">
            <h4>${product.productName}</h4>
          </div>
          <div class="quantity">
            <span class="quantity-btn-decrese">-</span>
            <span class="quantity-input">${product.quanty}</span>
            <span class="quantity-btn-increse">+</span>
          </div>
          <div class="price">${product.price * product.quanty} $</div>
          <div class="delete-product">❌</div>
        </div>
      `;
      modalContainer.append(modalBody);

      // Botones +/-
      const decrese = modalBody.querySelector(".quantity-btn-decrese");
      decrese.addEventListener("click", () => {
        if (product.quanty > 1) {
          product.quanty--;
          displayCart();
        }
        displayCartCounter();
      });

      const increse = modalBody.querySelector(".quantity-btn-increse");
      increse.addEventListener("click", () => {
        product.quanty++;
        displayCart();
        displayCartCounter();
      });

      // Eliminar
      const deleteProduct = modalBody.querySelector(".delete-product");
      deleteProduct.addEventListener("click", () => {
        deleteCartProduct(product.id);
      });
    });

    // Footer
    const total = cart.reduce((acc, el) => acc + el.price * el.quanty, 0);

    const modalFooter = document.createElement("div");
    modalFooter.className = "modal-footer";
    modalFooter.innerHTML = `
      <div class="total-price">Total: $${total}</div>
      <button class="btn-primary" id="checkout-btn">Ir a pago</button>
      <div id="button-checkout"></div>
    `;
    modalContainer.append(modalFooter);

    const checkoutButton = modalFooter.querySelector("#checkout-btn");

    checkoutButton.addEventListener("click", async function () {
      try {
        if (!mercadopago) {
          alert("No se pudo inicializar Mercado Pago. Ver consola.");
          return;
        }

        console.log("Botón 'Ir a pago' clickeado ✅");
        checkoutButton.disabled = true;

        const orderData = {
          quantity: 1,
          description: "compra de ecommerce",
          price: total,
        };

        const resp = await fetch("http://localhost:8080/create_preference", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(orderData),
        });

        if (!resp.ok) {
          const err = await resp.json().catch(() => ({}));
          console.error("Error del backend:", err);
          throw new Error("No se pudo crear la preferencia");
        }

        const preference = await resp.json();
        console.log("Preference creada:", preference);

        if (!preference?.id) {
          throw new Error("El backend no devolvió un 'id' de preferencia");
        }

        // Ocultar botón manual y crear el brick
        checkoutButton.remove();
        await createCheckoutButton(preference.id);
      } catch (error) {
        console.error("Error al iniciar el pago:", error);
        alert("Error al iniciar el pago. Revisá la consola.");
        checkoutButton.disabled = false;
      }
    });

    async function createCheckoutButton(preferenceId) {
      console.log("Inicializando Brick con preferenceId:", preferenceId);

      const bricksBuilder = mercadopago.bricks();
      await bricksBuilder.create("wallet", "button-checkout", {
        initialization: { preferenceId },
        customization: {
          texts: { valueProp: "smart_option" }, // "Pagá con MP"
        },
        callbacks: {
          onError: (error) => console.error("Error en el Brick:", error),
          onReady: () => console.log("Botón de Mercado Pago listo ✅"),
        },
      });
    }
  } else {
    const modalText = document.createElement("h2");
    modalText.className = "modal-body";
    modalText.innerText = "Su carrito está vacío";
    modalContainer.append(modalText);
  }
};

cartBtn.addEventListener("click", displayCart);

// Borrar producto
const deleteCartProduct = (id) => {
  const idx = cart.findIndex((el) => el.id === id);
  if (idx !== -1) {
    cart.splice(idx, 1);
    console.log("Producto eliminado, idx:", idx);
    displayCart();
    displayCartCounter();
  }
};

// Contador
const displayCartCounter = () => {
  const cartLength = cart.reduce((acc, el) => acc + el.quanty, 0);
  if (cartLength > 0) {
    cartCounter.style.display = "block";
    cartCounter.innerText = cartLength;
  } else {
    cartCounter.style.display = "none";
  }
};
