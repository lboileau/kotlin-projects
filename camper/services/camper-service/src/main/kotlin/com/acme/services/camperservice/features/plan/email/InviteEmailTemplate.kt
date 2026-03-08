package com.acme.services.camperservice.features.plan.email

object InviteEmailTemplate {

    fun subject(planName: String): String = "You're invited to $planName!"

    fun html(inviterName: String, planName: String, planUrl: String): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Georgia', 'Times New Roman', serif; background-color: #f4ede4;">
            <table width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4ede4; padding: 40px 20px;">
                <tr>
                    <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 24px rgba(0,0,0,0.08);">
                            <!-- Header Banner -->
                            <tr>
                                <td style="background: linear-gradient(135deg, #2d5016 0%, #4a7c23 40%, #e8913a 100%); padding: 48px 40px; text-align: center;">
                                    <div style="font-size: 48px; margin-bottom: 8px;">🏕️</div>
                                    <h1 style="color: #ffffff; font-size: 28px; margin: 0; font-weight: bold; text-shadow: 0 2px 4px rgba(0,0,0,0.2);">
                                        Adventure Awaits!
                                    </h1>
                                </td>
                            </tr>

                            <!-- Main Content -->
                            <tr>
                                <td style="padding: 40px;">
                                    <h2 style="color: #2d5016; font-size: 22px; margin: 0 0 16px 0; line-height: 1.4;">
                                        ${escapeHtml(inviterName)} is inviting you to join them on <span style="color: #e8913a;">${escapeHtml(planName)}</span>!
                                    </h2>
                                    <p style="color: #5a5a5a; font-size: 16px; line-height: 1.6; margin: 0 0 32px 0;">
                                        Join them to see all the action — check out the itinerary, sign up for tents and canoes, and help plan the gear list. The campfire's waiting! 🔥
                                    </p>

                                    <!-- CTA Button -->
                                    <table width="100%" cellpadding="0" cellspacing="0">
                                        <tr>
                                            <td align="center">
                                                <a href="$planUrl" style="display: inline-block; background: linear-gradient(135deg, #e8913a 0%, #d4782a 100%); color: #ffffff; text-decoration: none; padding: 16px 40px; border-radius: 8px; font-size: 18px; font-weight: bold; letter-spacing: 0.5px; box-shadow: 0 4px 12px rgba(232, 145, 58, 0.4);">
                                                    🌲 Check It Out
                                                </a>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>

                            <!-- Divider -->
                            <tr>
                                <td style="padding: 0 40px;">
                                    <hr style="border: none; border-top: 1px solid #e8ddd0; margin: 0;">
                                </td>
                            </tr>

                            <!-- Footer -->
                            <tr>
                                <td style="padding: 24px 40px 32px;">
                                    <p style="color: #9a9a9a; font-size: 13px; line-height: 1.5; margin: 0; font-style: italic;">
                                        This email is from Louis. If you have any concerns, please contact him directly.
                                    </p>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </body>
        </html>
    """.trimIndent()

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
