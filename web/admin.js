async function loadShows() {
  try {
    const a = await api('/api/admin/shows');

    $('showTable').innerHTML = `
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Movie</th>
              <th>Cinema</th>
              <th>Date</th>
              <th>Time</th>
              <th>Price</th>
              <th>Booked Seats</th>
              <th>Total Seats</th>
              <th>Available Seats</th>
            </tr>
          </thead>
          <tbody>
            ${a.map(x => {
              const booked = Number(x.booked ?? 0);
              const total = Number(x.total ?? 0);
              const available = Math.max(0, total - booked);

              return `
                <tr>
                  <td>${x.id ?? '-'}</td>
                  <td>${x.movie ?? '-'}</td>
                  <td>${x.theatre ?? '-'}</td>
                  <td>${x.date ?? '-'}</td>
                  <td>${x.time ?? '-'}</td>
                  <td>₹${Number(x.price ?? 0)}</td>
                  <td>${booked}</td>
                  <td>${total}</td>
                  <td>${available}</td>
                </tr>
              `;
            }).join('')}
          </tbody>
        </table>
      </div>
    `;
  } catch (error) {
    console.error('Failed to load shows:', error);
    toast('Shows load aagala: ' + error.message);
  }
}