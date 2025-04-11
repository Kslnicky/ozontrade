const globalTrack = document.querySelectorAll(".features__companies-wrapper")
const trackOne = document.getElementById("track-one")

function getChildrenSumWidth({element, columnGap}) {
    let i = 0
    for (const child of element.children) {
        i += child.scrollWidth + columnGap
    }
    startAnimation(element,i / 2)
}

function startAnimation(element, value) {
    element.style.setProperty('--x', `-${value}px`)
}

function changeGap() {
    for (const child of globalTrack) {
        child.style.columnGap = `${columnGapValue}px`
    }
}

let columnGapValue = 0

if( /Android|webOS|iPhone|iPad|iPod|BlackBerry/i.test(navigator.userAgent) ) {
    columnGapValue = 111
    changeGap()
} else {
    columnGapValue = 111
    changeGap()
}

setTimeout(getChildrenSumWidth, 100, {element: trackOne, columnGap: columnGapValue})