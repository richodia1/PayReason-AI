const urls = [
  `http://127.0.0.1:${process.env.API_PORT || 8180}/actuator/health`,
  `http://127.0.0.1:${process.env.AI_PORT || 8181}/capabilities`,
  `http://127.0.0.1:${process.env.WEB_PORT || 5180}/`,
]
for (const url of urls) {
  let ready = false
  for (let attempt = 0; attempt < 90; attempt++) {
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(2000) })
      if (response.ok && (!url.endsWith('/capabilities') || (await response.json()).scoring_available === true)) { ready = true; break }
    } catch { /* Startup is still in progress. */ }
    await new Promise(resolve => setTimeout(resolve, 1000))
  }
  if (!ready) throw new Error(`Service not ready: ${url}`)
  console.log(`Ready: ${url}`)
}
