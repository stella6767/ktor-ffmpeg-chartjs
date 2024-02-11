const ctx = document.getElementById('myChart');


new Chart(ctx, {
    type: 'line',
    data: {
        labels: xValues,
        datasets: [{
            label: 'Waveform',
            data: yValues,
            borderColor: 'blue',
            borderWidth: 1,
            fill: false
        }]
    },
    options: {
        responsive: false, // Adjust as needed
        scales: {
            x: {
                title: {
                    display: true,
                    text: 'Time'
                }
            },
            y: {
                title: {
                    display: true,
                    text: 'Amplitude'
                }
            }
        }
    }
});