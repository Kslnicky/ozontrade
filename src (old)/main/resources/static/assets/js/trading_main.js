// Tabs init start

new tabsRush('btnBox',{
    tabs: [
        {
            btn: 'btnOne',
            item: 'ConOne'
        },
        {
            btn: 'btnTwo',
            item: 'ConTwo'
        },
        {
            btn: 'btnThree',
            item: 'ConThree'
        }
    ],
    buttonActive : 'buttonActiveNew',
    itemActive : 'itemActiveNew'
})


new tabsRush('orders',{
    tabs: [
        {
            btn: 'ordersBtnOne',
            item: 'ordersConOne'
        },
        {
            btn: 'ordersBtnTwo',
            item: 'ordersConTwo'
        }
    ],
    buttonActive : 'buttonActiveNew',
    itemActive : 'itemActiveNew'
})

// Tabs init finish

// Selects start

const tradeSelects = (selector, list) => {
    const select = document.querySelector(selector)
    const selectList = document.querySelector(list)
    select.addEventListener('click', (event) => {
        if (!select.classList.contains("--active-select")) {
            remoteActiveSelects()
            select.classList.toggle('--active-select')
            selectList.classList.toggle('--active-select')
        } else {
            remoteActiveSelects()
        }

        if (event.target.classList.contains("order__limit-item")) {
            select.children[0].innerText = event.target.innerText
        }
    })
}

const remoteActiveSelects = () => {
    const allActiveClasses = document.querySelectorAll(".--active-select")
    allActiveClasses.forEach(active => active.classList.remove("--active-select"))
}

document.body.addEventListener("click", (event) => {
    if (!event.target.classList.contains("order__limit-select")) {
        remoteActiveSelects()
    }
})


tradeSelects('.tradeSelectOne', '.tradeListOne')
tradeSelects('.tradeSelectTwo', '.tradeListTwo')