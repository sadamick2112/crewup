import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { expectNoAxeViolations } from "@/test/axe";
import { Button } from "./button";

// Smoke test for the frontend tooling: shadcn/ui, Testing Library and axe working together.
describe("shadcn Button", () => {
  it("M0_10_buttonRendersAndRespondsToClicks", async () => {
    const onClick = vi.fn();
    render(<Button onClick={onClick}>Sign in</Button>);

    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(onClick).toHaveBeenCalledOnce();
  });

  it("M0_25_buttonHasNoAxeViolations", async () => {
    const { container } = render(<Button>Sign in</Button>);

    await expectNoAxeViolations(container);
  });
});
