import { NextResponse } from 'next/server';

export async function POST(req: Request) {
  const body = await req.json();
  const { app_name, package_name, webview_url } = body;

  const GITHUB_TOKEN = process.env.GITHUB_TOKEN; 
  const GITHUB_OWNER = 'jahidul0p'; // আপনার GitHub ইউজারনেম
  const GITHUB_REPO = 'MyMonitorApp'; // আপনার অ্যাপের রিপোর নাম

  const url = `https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/actions/workflows/build.yml/dispatches`;
  
  await fetch(url, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${GITHUB_TOKEN}`,
      'Content-Type': 'application/json',
      'Accept': 'application/vnd.github.v3+json',
    },
    body: JSON.stringify({
      ref: 'main',
      inputs: { app_name, package_name, webview_url }
    })
  });

  return NextResponse.json({ success: true, message: "🚀 APK বিল্ডিং গিটহাবে শুরু হয়েছে!" });
}
