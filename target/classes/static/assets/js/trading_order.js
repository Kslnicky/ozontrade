function changeBackgroundInput(element, value, dot) {
    const input = document.querySelector(`.${element}`);
    const valueInput = document.querySelector(`.${value}`);
    const valueDot = document.querySelector(`.${dot}`);

    input.value = 0

    input.addEventListener('input', function() {
        const value = this.value;
        valueInput.style.left = `calc(${value}% - (${value * 0.2}px))`
        valueDot.style.left = `calc(${value}% - (${value * 0.150}px))`
        valueInput.innerText = `${value}%`
        valueInput.classList.add("active")
        this.style.background = `linear-gradient(to right, #f8bb3a 0%, #f8bb3a ${value}%, rgba(248, 187, 58, 0.34) ${value}%, rgba(248, 187, 58, 0.34) 100%)`;
    })

    input.addEventListener('mouseenter', function () {
        valueInput.classList.add("active")
    })

    input.addEventListener('mouseleave', function () {
        valueInput.classList.remove("active")
    })

    window.addEventListener("scroll", () => {
        valueInput.classList.remove("active")
    })

    document.body.addEventListener("click", () => {
        valueInput.classList.remove("active")
    })
}

changeBackgroundInput("order__slider-buy-limit", "order__slider-buy-value-limit", "order__slider-buy-dot-limit")
changeBackgroundInput("order__slider-sell-limit", "order__slider-sell-value-limit", "order__slider-sell-dot-limit")

changeBackgroundInput("order__slider-buy-market", "order__slider-buy-value-market", "order__slider-buy-dot-market")
changeBackgroundInput("order__slider-sell-market", "order__slider-sell-value-market", "order__slider-sell-dot-market")

changeBackgroundInput("order__slider-buy-order", "order__slider-buy-value-order", "order__slider-buy-dot-order")
changeBackgroundInput("order__slider-sell-order", "order__slider-sell-value-order", "order__slider-sell-dot-order")
